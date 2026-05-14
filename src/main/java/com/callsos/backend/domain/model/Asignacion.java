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
import lombok.Getter;
 
import java.time.LocalDateTime;
 
/**
 * Registro que vincula un Agente con un Incidente en un momento dado.
 * Ciclo de vida: ACTIVA → FINALIZADA.
 */
@Getter
public class Asignacion {
 
    private final String id;                // UUID
    private final LocalDateTime fechaAsignacion;
    private EstadoAsignacion estado;
    private final Agente agente;
    private final Incidente incidente;
 
    public Asignacion(String id, Agente agente, Incidente incidente) {
        this.id              = id;
        this.agente          = agente;
        this.incidente       = incidente;
        this.fechaAsignacion = LocalDateTime.now();
        this.estado          = EstadoAsignacion.ACTIVA;
    }
 
    /**
     * Finaliza la asignación y libera al agente.
     * Solo puede ejecutarse si la asignación está ACTIVA.
     */
    public void finalizar() {
        if (EstadoAsignacion.FINALIZADA.equals(this.estado))
            throw new IllegalStateException("La asignación ya está finalizada.");
        this.estado = EstadoAsignacion.FINALIZADA;
        this.agente.liberar();
    }
}