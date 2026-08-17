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
import java.time.LocalDateTime;
/**
 * Evento de dominio base para cambios en el ciclo de vida del Incidente.
 *
 * Los eventos de dominio modelan hechos que ya ocurrieron.
 * Son inmutables y llevan el contexto mínimo necesario para que
 * los listeners puedan actuar sin consultar el repositorio.
 *
 * Flujo:
 *   Caso de uso → publica evento → ApplicationEventPublisher
 *   → Spring notifica → @EventListener en adaptadores de infraestructura
 *
 * El dominio publica. La infraestructura escucha.
 * El dominio nunca conoce Firebase ni WebSocket.
 *
 * Épica 2 (fix P5): antes este evento no llevaba el estado ANTERIOR, así
 * que AuditoriaEventListener siempre registraba estadoAnterior = null,
 * sin importar la transición real. Ahora cada caso de uso captura
 * incidente.getEstado() ANTES de mutar el agregado y lo pasa acá.
 */
public class IncidenteEvent {
    
     private final String incidenteId;
    private final String denuncianteId;
    private final EstadoIncidente estadoAnterior;  // null solo si es la creación inicial
    private final EstadoIncidente estadoNuevo;
    private final LocalDateTime ocurridoEn;
 
    public IncidenteEvent(String incidenteId, String denuncianteId,
                          EstadoIncidente estadoAnterior, EstadoIncidente estadoNuevo) {
        this.incidenteId   = incidenteId;
        this.denuncianteId = denuncianteId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo   = estadoNuevo;
        this.ocurridoEn    = LocalDateTime.now();
    }
 
    public String getIncidenteId()       { return incidenteId; }
    public String getDenuncianteId()     { return denuncianteId; }
    public EstadoIncidente getEstadoAnterior() { return estadoAnterior; }
    public EstadoIncidente getEstadoNuevo() { return estadoNuevo; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
}
