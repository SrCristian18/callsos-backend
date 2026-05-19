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
 * Clase ternaria: surge de la relación entre Incidente y AutoridadPolicial.
 *
 * Regla de negocio: no puede existir un ReporteAdministrativo si no hay
 * un Incidente y una AutoridadPolicial. Sirve para análisis y seguimiento
 * estadístico a nivel de mando.
 */

public class ReporteAdministrativo {
 
    private final String id;                      // UUID
    private final LocalDateTime fecha;
    private final String resumen;
    private final Incidente incidente;              // OBLIGATORIO
    private final AutoridadPolicial autoridad;      // OBLIGATORIO
    
    public ReporteAdministrativo(String id, String resumen,
                                 Incidente incidente,
                                 AutoridadPolicial autoridad) {
        Objects.requireNonNull(incidente,
            "Un ReporteAdministrativo requiere un Incidente.");
        Objects.requireNonNull(autoridad,
            "Un ReporteAdministrativo requiere una AutoridadPolicial.");
 
        this.id        = id;
        this.resumen   = resumen;
        this.incidente = incidente;
        this.autoridad = autoridad;
        this.fecha     = LocalDateTime.now();
    }
 
    public String getId()                       { return id; }
    public LocalDateTime getFecha()             { return fecha; }
    public String getResumen()                  { return resumen; }
    public Incidente getIncidente()             { return incidente; }
    public AutoridadPolicial getAutoridad()     { return autoridad; }
}
