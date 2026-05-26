/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.enums.EstadoAsignacion;
 
import java.time.LocalDateTime;
import java.util.Objects;
 
/**
/**
 * Clase ternaria: surge de la Denuncia.
 *
 * Regla de negocio: sin una Denuncia no puede existir una Asignacion.
 *
 * CONSTRUCTORES:
 *   - Asignacion(id, agente, denuncia)  → creación nueva, dispara agente.asignar()
 *   - reconstituir(...)                 → reconstrucción desde BD, sin efectos de dominio
 *
 * La separación entre creación y reconstitución es el patrón correcto en DDD:
 * los invariantes de negocio solo aplican al crear, no al reconstruir.
 */

public class Asignacion {
 
    private final String id;                // UUID
    private final LocalDateTime fechaAsignacion;
    private EstadoAsignacion estado;
    private final Agente agente;
    private final Denuncia denuncia; // OBLIGATORIO — origen de la asignación
 
    // ── Constructor de CREACIÓN — con efectos de dominio ──────────────────
    public Asignacion(String id, Agente agente, Denuncia denuncia) {
        
        Objects.requireNonNull(denuncia,
            "Una Asignacion requiere una Denuncia. Sin Denuncia no hay Asignacion.");
        Objects.requireNonNull(agente,
            "Una Asignacion requiere un Agente.");
 
        if (!agente.estaDisponible())
            throw new IllegalStateException(
                "El agente '" + agente.getNombre() + "' no está disponible para ser asignado.");
        this.id              = id;
        this.agente          = agente;
        this.denuncia       = denuncia;
        this.fechaAsignacion = LocalDateTime.now();
        this.estado          = EstadoAsignacion.ACTIVA;
    
        // Efecto de dominio inmediato: el agente queda ocupado
        this.agente.asignar();
    }
    
    // ── Constructor de RECONSTITUCIÓN — sin efectos de dominio ────────────
 
    /**
     * Reconstruye una Asignacion desde la capa de persistencia.
     *
     * NO valida disponibilidad del agente ni llama a agente.asignar().
     * El estado ya existe en BD — solo se restaura en memoria.
     * EXCLUSIVO para uso de adaptadores de persistencia.
     */
    private Asignacion(String id, LocalDateTime fechaAsignacion,
                       EstadoAsignacion estado, Agente agente, Denuncia denuncia) {
        this.id              = id;
        this.fechaAsignacion = fechaAsignacion;
        this.estado          = estado;
        this.agente          = agente;
        this.denuncia        = denuncia;
    }
    
    /**
     * Factory method de reconstitución — punto de entrada para los adaptadores.
     * Uso: Asignacion.reconstituir(id, fecha, estado, agente, denuncia)
     */
    public static Asignacion reconstituir(String id, LocalDateTime fechaAsignacion,
                                          EstadoAsignacion estado,
                                          Agente agente, Denuncia denuncia) {
        Objects.requireNonNull(id, "ID de asignación requerido.");
        Objects.requireNonNull(agente, "Agente requerido para reconstitución.");
        Objects.requireNonNull(estado, "Estado requerido para reconstitución.");
        return new Asignacion(id, fechaAsignacion, estado, agente, denuncia);
    }
    
    // ── Comportamiento de dominio ──────────────────────────────────────────
    /**
     * Finaliza la asignación y libera al agente.
     * Solo puede ejecutarse si el estado es ACTIVA.
     */
    public void finalizar() {
        if (EstadoAsignacion.FINALIZADA.equals(this.estado))
            throw new IllegalStateException("La asignación ya fue finalizada.");
        this.estado = EstadoAsignacion.FINALIZADA;
        this.agente.liberar();
    }
 
    // ── Getters ────────────────────────────────────────────────────────────
    /** Atajo de conveniencia: incidente al que responde esta asignación. */
    public Incidente getIncidente() {
        return denuncia.getIncidente();
    }
    
    public String getId()                     { return id; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public EstadoAsignacion getEstado()       { return estado; }
    public Agente getAgente()                 { return agente; }
    public Denuncia getDenuncia()             { return denuncia; }
}