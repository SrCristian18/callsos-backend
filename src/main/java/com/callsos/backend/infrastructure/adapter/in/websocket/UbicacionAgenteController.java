/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.websocket;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.in.PublicarUbicacionAgentePort;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.ruta.SimulacionEstado;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Adaptador de entrada WebSocket: recibe posiciones GPS del agente
 * y las transmite en tiempo real a quien esté autorizado a verlas.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  App Flutter (AGENTE)                                       │
 * │    → SEND /app/ubicacion/{incidenteId}                      │
 * │    → payload: { agenteId, latitud, longitud }               │
 * └────────────────────────┬────────────────────────────────────┘
 *                          │ @MessageMapping
 *                          ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  UbicacionAgenteController                                  │
 * │    1. Valida rol AGENTE + usa el agenteId del JWT            │
 * │    2. Delega en PublicarUbicacionAgentePort                 │
 * │       (persiste + publica en /topic/agente/{agenteId}/...)  │
 * └────────────────────────┬────────────────────────────────────┘
 *                          │ SimpMessagingTemplate
 *                          ▼
 * ┌─────────────────────────────────────────────────────────────┐
 *  Suscritos a /topic/agente/{agenteId}/ubicacion:
 *    el propio AGENTE, OPERADOR_CAI de su unidad, COMANDO.
 *    ← reciben { latitud, longitud, timestamp }
 *
 *  Épica 3 (fix P6): el DENUNCIANTE NUNCA puede suscribirse a este
 *  topic — StompAuthChannelInterceptor rechaza el SUBSCRIBE si el rol
 *  no está autorizado (VerificarAccesoTrackingService). Antes de esta
 *  épica, el topic era "/topic/incidente/{id}/ubicacion" — nombrado por
 *  incidente, no por agente — y cualquier cliente autenticado (sin
 *  importar el rol) podía suscribirse solo con conocer el incidenteId,
 *  incluido el propio denunciante dueño del incidente.
 * └─────────────────────────────────────────────────────────────┘
 */
@Controller
public class UbicacionAgenteController {
    
    private static final Logger log = LoggerFactory.getLogger(UbicacionAgenteController.class);
    private final PublicarUbicacionAgentePort publicarUbicacion;
    private final SimulacionEstado simulacionEstado;
    private final UbicacionAgenteRepositoryPort repositorio;
    private final SimpMessagingTemplate messagingTemplate;
  
    public UbicacionAgenteController(PublicarUbicacionAgentePort publicarUbicacion, SimulacionEstado simulacionEstado,
            UbicacionAgenteRepositoryPort repositorio, SimpMessagingTemplate messagingTemplate) {
        this.publicarUbicacion = publicarUbicacion;
        this.simulacionEstado = simulacionEstado;
        this.repositorio = repositorio;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Recibe la posición GPS del agente y la retransmite a quien esté
     * autorizado a verla (ver StompAuthChannelInterceptor).
     *
     * Si el incidente tiene una simulación de recorrido ACTIVA (modo prueba),
     * esta posición real se descarta: el backend ya está reproduciendo el
     * trayecto simulado y no queremos que ambas fuentes compitan por el
     * mismo topic STOMP.
     * 
     * @param incidenteId  ID del incidente — sigue siendo parte del destino
     *                     de este SEND (el agente reporta su posición EN
     *                     EL CONTEXTO de un incidente concreto), pero el
     *                     topic de SALIDA (broadcast) ya no se nombra por
     *                     incidenteId — ver PublicarUbicacionAgentePort.
     * @param payload      Posición enviada por la app del agente
     */
    @MessageMapping("/ubicacion/{incidenteId}")
    public void recibirUbicacion(
            @DestinationVariable String incidenteId,
            @Payload UbicacionPayload payload,
            Principal principal) {

        if (simulacionEstado.estaSimulando(incidenteId))
        {
            log.debug("Ubicacion real ignorada - incidente {} está en modo simulacion.",incidenteId);
            return;
        }

        // Spring inyecta aca el Principal que StompAuthChannelInterceptor
        // seteo en el frame CONNECT (accessor.setUser(...)). No puede ser
        // null si el interceptor esta registrado, pero se valida por
        // defensividad ante cambios futuros de configuracion.
        if (principal == null) {
            throw new IllegalStateException(
                "No hay Principal autenticado en la sesion STOMP");
        }

        // El agenteId viene del TOKEN, no del payload que manda el cliente.
        // Antes: se confiaba en payload.agenteId(), lo que permitia que
        // cualquier cliente conectado enviara ubicaciones a nombre de
        // OTRO agente con solo cambiar ese campo en el JSON.
        String agenteIdAutenticado = principal.getName();

        // Solo un usuario con rol AGENTE puede publicar su ubicacion.
        // (SecurityConfig no cubre destinos STOMP como /app/**, por eso
        // esta verificacion de rol se hace aca explicitamente).
        if (principal instanceof Authentication auth) {
            boolean esAgente = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_AGENTE"::equals);

            if (!esAgente) {
                throw new IllegalStateException(
                    "Solo un AGENTE puede enviar su ubicacion. Rol actual no autorizado.");
            }
        }

        // Construir el value object — Ubicacion valida los rangos de lat/lon
        Ubicacion ubicacion = new Ubicacion(payload.latitud(), payload.longitud());        
        
        // FIX: se estaba pasando payload.agenteId() (dato no confiable del
        // cliente) en vez de agenteIdAutenticado (extraído del JWT arriba).
        // Esto reintroducía silenciosamente la vulnerabilidad de suplantación
        // que el comentario de agenteIdAutenticado ya documentaba como
        // resuelta — cualquier agente conectado podía publicar ubicaciones
        // a nombre de OTRO agente con solo cambiar el campo en el JSON.
        publicarUbicacion.publicar(agenteIdAutenticado, incidenteId, ubicacion);
    }

    /**
     * Recupera la última posición conocida de un agente para un incidente
     * (ej: al reabrir la app y reconectar el WebSocket).
     *
     * Épica 3: el destino de publicación ya no se nombra por incidenteId
     * ("/topic/incidente/{id}/ubicacion") sino por agenteId
     * ("/topic/agente/{agenteId}/ubicacion") — igual que
     * PublicarUbicacionAgenteService. La protección real de "quién puede
     * ver esto" es la autorización de SUBSCRIBE en
     * StompAuthChannelInterceptor: el DENUNCIANTE nunca logra suscribirse
     * a ese topic, así que aunque este SEND siga siendo invocable por
     * cualquiera, no hay fuga — nadie no autorizado puede estar del otro
     * lado recibiendo el broadcast.
     */
    @MessageMapping("/ubicacion/{incidenteId}/ultima")
    public void solicitarUltimaPosicion(
            @DestinationVariable String incidenteId,
            @Payload UltimaUbicacionRequest request) {
 
        repositorio.ultimaPosicion(request.agenteId(), incidenteId)
            .ifPresent(ua -> messagingTemplate.convertAndSend(
                "/topic/agente/" + request.agenteId() + "/ubicacion",
                new UbicacionResponse(
                    ua.getUbicacion().getLatitud(),
                    ua.getUbicacion().getLongitud(),
                    ua.getTimestamp().toString()
                )
            ));
    }
 
    // ── Records Java 21 como DTOs de WebSocket ─────────────────────────────
 
    /**
     * Payload recibido del agente.
     *
     * agenteId se mantiene en el DTO por compatibilidad con el payload
     * que ya envia el cliente Flutter, pero NO se usa como fuente de
     * verdad — ver recibirUbicacion(): el agenteId real sale del JWT
     * (Principal), no de este campo. Queda pendiente coordinar con el
     * frontend para eventualmente retirarlo del payload y limpiar este
     * campo tambien.
     */
    public record UbicacionPayload(
        String agenteId,
        double latitud,
        double longitud
    ) {}
 
    /** Payload enviado al denunciante. */
    public record UbicacionResponse(
        double latitud,
        double longitud,
        String timestamp
    ) {}
 
    /** Request del denunciante para recuperar última posición. */
    public record UltimaUbicacionRequest(String agenteId) {}
}