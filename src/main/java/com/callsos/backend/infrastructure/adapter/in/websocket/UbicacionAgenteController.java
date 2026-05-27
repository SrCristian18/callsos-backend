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
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
    
     private final UbicacionAgenteRepositoryPort repositorio;
    private final SimpMessagingTemplate messagingTemplate;
 
    public UbicacionAgenteController(UbicacionAgenteRepositoryPort repositorio,
                                     SimpMessagingTemplate messagingTemplate) {
        this.repositorio       = repositorio;
        this.messagingTemplate = messagingTemplate;
    }
 
    /**
     * Recibe la posición GPS del agente y la retransmite al denunciante.
     *
     * @param incidenteId  ID del incidente — parte del destino STOMP
     * @param payload      Posición enviada por la app del agente
     */
    @MessageMapping("/ubicacion/{incidenteId}")
    public void recibirUbicacion(
            @DestinationVariable String incidenteId,
            @Payload UbicacionPayload payload) {
 
        // Construir el value object — Ubicacion valida los rangos de lat/lon
        Ubicacion ubicacion = new Ubicacion(payload.latitud(), payload.longitud());
 
        UbicacionAgente ua = new UbicacionAgente(
            payload.agenteId(),
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
 
    /** Payload recibido del agente. */
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
