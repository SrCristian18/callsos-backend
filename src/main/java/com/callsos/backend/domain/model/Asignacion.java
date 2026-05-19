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
 * Clase ternaria: surge de la Denuncia.
 *
 * Regla de negocio: sin una Denuncia no puede existir una Asignacion.
 * La Denuncia ya garantiza la existencia de Denunciante e Incidente,
 * por lo que la Asignacion hereda transitivamente esas dependencias.
 *
 * Al crearse, ocupa al Agente (lo marca como OCUPADO).
 * Al finalizar, libera al Agente (lo marca como DISPONIBLE).
 *
 * Ciclo de vida: ACTIVA → FINALIZADA
 */

public class Asignacion {
 
    private final String id;                // UUID
    private final LocalDateTime fechaAsignacion;
    private EstadoAsignacion estado;
    private final Agente agente;
    private final Denuncia denuncia; // OBLIGATORIO — origen de la asignación
 
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
 
    // ── Acceso al Incidente y Denunciante a través de la Denuncia ──────────
 
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