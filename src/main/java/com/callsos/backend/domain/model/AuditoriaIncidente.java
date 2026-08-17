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
 * Registro inmutable de un hecho auditable del incidente.
 *
 * Épica 2 — modelo generalizado (decisión: ALTER TABLE, no tabla nueva):
 * este mismo registro ahora expresa DOS clases de hechos distintos:
 *
 *   1. Transición de estado (uso original, Fase 3):
 *      estadoAnterior/estadoNuevo llevan la transición real,
 *      campo/valorAnteriorGenerico/valorNuevoGenerico quedan NULL.
 *
 *   2. Cambio de un campo genérico que NO es una transición de estado
 *      (ej. "tipo" cambia de ROBOS_O_ASALTOS a RIÑAS_O_PELEAS):
 *      campo/valorAnteriorGenerico/valorNuevoGenerico llevan el hecho,
 *      estadoAnterior queda NULL y estadoNuevo lleva el estado VIGENTE
 *      del incidente al momento del evento (no representa una
 *      transición — existe solo porque la columna es NOT NULL).
 *
 * Se optó por generalizar este mismo modelo en vez de crear uno paralelo
 * para no duplicar la tabla de trazabilidad ni forzar a los consumidores
 * (AuditoriaController, Flutter) a consultar dos fuentes distintas para
 * reconstruir el historial completo de un incidente.
 */
public class AuditoriaIncidente {
    
     private final String incidenteId;
    private final EstadoIncidente estadoAnterior;  // null si es la creación inicial o un cambio de campo genérico
    private final EstadoIncidente estadoNuevo;
    private final String actorId;
    private final String actorRol;
    private final LocalDateTime timestamp;
    private final String detalle;
    private final String campo;                    // null si es un cambio de estado normal
    private final String valorAnteriorGenerico;     // null si es un cambio de estado normal
    private final String valorNuevoGenerico;        // null si es un cambio de estado normal

    /** Constructor original — registra una transición de estado. */
    public AuditoriaIncidente(String incidenteId,
                              EstadoIncidente estadoAnterior,
                              EstadoIncidente estadoNuevo,
                              String actorId,
                              String actorRol,
                              String detalle) {
        this(incidenteId, estadoAnterior, estadoNuevo, actorId, actorRol, detalle,
             null, null, null);
    }

    /**
     * Constructor genérico (Épica 2) — registra una transición de estado
     * O un cambio de campo genérico, según se completen o no los últimos
     * 3 parámetros.
     */
    public AuditoriaIncidente(String incidenteId,
                              EstadoIncidente estadoAnterior,
                              EstadoIncidente estadoNuevo,
                              String actorId,
                              String actorRol,
                              String detalle,
                              String campo,
                              String valorAnteriorGenerico,
                              String valorNuevoGenerico) {
        this.incidenteId            = incidenteId;
        this.estadoAnterior         = estadoAnterior;
        this.estadoNuevo            = estadoNuevo;
        this.actorId                = actorId;
        this.actorRol               = actorRol;
        this.detalle                = detalle;
        this.campo                  = campo;
        this.valorAnteriorGenerico  = valorAnteriorGenerico;
        this.valorNuevoGenerico     = valorNuevoGenerico;
        this.timestamp              = LocalDateTime.now();
    }

    /**
     * Fábrica explícita para un cambio de campo genérico — evita que el
     * caller tenga que recordar el orden/significado de los últimos 3
     * parámetros del constructor cuando NO es un cambio de estado.
     */
    public static AuditoriaIncidente deCambioGenerico(String incidenteId,
                                                       EstadoIncidente estadoVigente,
                                                       String actorId,
                                                       String actorRol,
                                                       String detalle,
                                                       String campo,
                                                       String valorAnterior,
                                                       String valorNuevo) {
        return new AuditoriaIncidente(incidenteId, null, estadoVigente,
            actorId, actorRol, detalle, campo, valorAnterior, valorNuevo);
    }

    public String getIncidenteId()             { return incidenteId; }
    public EstadoIncidente getEstadoAnterior() { return estadoAnterior; }
    public EstadoIncidente getEstadoNuevo()    { return estadoNuevo; }
    public String getActorId()                 { return actorId; }
    public String getActorRol()                { return actorRol; }
    public LocalDateTime getTimestamp()        { return timestamp; }
    public String getDetalle()                 { return detalle; }
    public String getCampo()                   { return campo; }
    public String getValorAnteriorGenerico()   { return valorAnteriorGenerico; }
    public String getValorNuevoGenerico()      { return valorNuevoGenerico; }

    /** true si este registro es un cambio de campo genérico (no una transición de estado). */
    public boolean esCambioGenerico() {
        return campo != null;
    }
}
