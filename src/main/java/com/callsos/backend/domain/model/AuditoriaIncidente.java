/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import java.time.LocalDateTime;
 
/**
 * Registro inmutable de una transición de estado del incidente.
 * Se crea automáticamente cada vez que el estado del incidente cambia,
 * capturando quién hizo el cambio y cuándo.
 */
public class AuditoriaIncidente {
    
     private final String incidenteId;
    private final EstadoIncidente estadoAnterior;  // null si es la creación inicial
    private final EstadoIncidente estadoNuevo;
    private final String actorId;
    private final String actorRol;
    private final LocalDateTime timestamp;
    private final String detalle;
 
    public AuditoriaIncidente(String incidenteId,
                              EstadoIncidente estadoAnterior,
                              EstadoIncidente estadoNuevo,
                              String actorId,
                              String actorRol,
                              String detalle) {
        this.incidenteId    = incidenteId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo    = estadoNuevo;
        this.actorId        = actorId;
        this.actorRol       = actorRol;
        this.detalle        = detalle;
        this.timestamp      = LocalDateTime.now();
    }
 
    public String getIncidenteId()             { return incidenteId; }
    public EstadoIncidente getEstadoAnterior() { return estadoAnterior; }
    public EstadoIncidente getEstadoNuevo()    { return estadoNuevo; }
    public String getActorId()                 { return actorId; }
    public String getActorRol()                { return actorRol; }
    public LocalDateTime getTimestamp()        { return timestamp; }
    public String getDetalle()                 { return detalle; }
}
