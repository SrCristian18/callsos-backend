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
 * Evento disparado cuando el incidente se finaliza o cancela.
 * Activa: cierre del canal WebSocket + notificación push al denunciante.
 */
public class IncidenteFinalizadoEvent extends IncidenteEvent{
    
    public IncidenteFinalizadoEvent(String incidenteId, String denuncianteId,
                                    EstadoIncidente estadoAnterior, EstadoIncidente estado) {
        super(incidenteId, denuncianteId, estadoAnterior, estado);
    }
}