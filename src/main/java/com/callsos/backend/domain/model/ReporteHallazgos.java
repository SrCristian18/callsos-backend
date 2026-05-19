/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import java.time.LocalDateTime;
import java.util.Objects;
 
/**
 * Clase ternaria: surge de la relación entre Incidente y Agente.
 *
 * Regla de negocio: no puede existir un ReporteHallazgos si no hay
 * un Incidente y un Agente. Documenta los hallazgos en campo
 * registrados por el Agente al atender el Incidente.
 */

public class ReporteHallazgos {
 
    private final String id;                // UUID
    private final LocalDateTime fecha;
    private final String descripcion;
    private final Incidente incidente;// OBLIGATORIO
    private final Agente agente;            // OBLIGATORIO

    public ReporteHallazgos(String id, String descripcion,
                            Incidente incidente, Agente agente) {
        Objects.requireNonNull(incidente,
            "Un ReporteHallazgos requiere un Incidente.");
        Objects.requireNonNull(agente,
            "Un ReporteHallazgos requiere un Agente.");
 
        this.id          = id;
        this.descripcion = descripcion;
        this.incidente   = incidente;
        this.agente      = agente;
        this.fecha       = LocalDateTime.now();
    }
 
    public String getId()             { return id; }
    public LocalDateTime getFecha()   { return fecha; }
    public String getDescripcion()    { return descripcion; }
    public Incidente getIncidente()   { return incidente; }
    public Agente getAgente()         { return agente; }
}
