/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.event;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.enums.EstadoIncidente;

/**
 * Evento disparado cuando el agente confirma que va en camino.
 *
 * Este evento activa en Fase 2:
 *   1. Notificación push al denunciante vía Firebase FCM
 *   2. Apertura del canal WebSocket de tracking GPS
 *
 * Extiende IncidenteEvent para llevar también el ID del agente,
 * necesario para que el WebSocket sepa qué posiciones transmitir.
 */
public class AgenteEnCaminoEvent extends IncidenteEvent{
    
    private final String agenteId;
 
    public AgenteEnCaminoEvent(String incidenteId, String denuncianteId,
                               EstadoIncidente estadoAnterior, String agenteId) {
        super(incidenteId, denuncianteId, estadoAnterior, EstadoIncidente.AGENTE_EN_CAMINO);
        this.agenteId = agenteId;
    }
 
    public String getAgenteId() { return agenteId; }
}
