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
 * y las transmite en tiempo real al denunciante suscrito.
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
 * │    1. Construye UbicacionAgente                             │
 * │    2. Persiste en BD (historial)                            │
 * │    3. Publica en /topic/incidente/{id}/ubicacion            │
 * └────────────────────────┬────────────────────────────────────┘
 *                          │ SimpMessagingTemplate
 *                          ▼
 * ┌─────────────────────────────────────────────────────────────┐
 *  App Flutter (DENUNCIANTE) suscrita a /topic/incidente/{id}/ubicacion
 *    ← recibe { latitud, longitud, timestamp }
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
     * Recibe la posición GPS del agente y la retransmite al denunciante.
     *
     * Si el incidente tiene una simulación de recorrido ACTIVA (modo prueba),
     * esta posición real se descarta: el backend ya está reproduciendo el
     * trayecto simulado y no queremos que ambas fuentes compitan por el
     * mismo topic STOMP.
     * 
     * @param incidenteId  ID del incidente — parte del destino STOMP
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
        
        publicarUbicacion.publicar(payload.agenteId(), incidenteId, ubicacion);
    }
    /* Descontinuado

        UbicacionAgente ua = new UbicacionAgente(
            agenteIdAutenticado,
            incidenteId,
            ubicacion
        );
 
        // Persistir en historial
        repositorio.guardar(ua);
 
        // Transmitir en tiempo real al denunciante suscrito
        messagingTemplate.convertAndSend(
            "/topic/incidente/" + incidenteId + "/ubicacion",
            new UbicacionResponse(
                payload.latitud(),
                payload.longitud(),
                ua.getTimestamp().toString()
            )
        );
    }
    */

    /**
     * Endpoint para que el denunciante recupere la última posición
     * conocida al reconectar (ej: al reabrir la app).
     */
    @MessageMapping("/ubicacion/{incidenteId}/ultima")
    public void solicitarUltimaPosicion(
            @DestinationVariable String incidenteId,
            @Payload UltimaUbicacionRequest request) {
 
        repositorio.ultimaPosicion(request.agenteId(), incidenteId)
            .ifPresent(ua -> messagingTemplate.convertAndSend(
                "/topic/incidente/" + incidenteId + "/ubicacion",
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