/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.enums.EstadoAgente;
import lombok.Getter;
 
/**
 * Agente de policía que puede ser asignado a un incidente.
 * Pertenece a una UnidadPolicial (composición, diagrama 2).
 */
@Getter
public class Agente {
 
    private final String id;       // UUID
    private String nombre;
    private EstadoAgente estado;
    private UnidadPolicial unidadPolicial;
 
    public Agente(String id, String nombre, UnidadPolicial unidadPolicial) {
        this.id             = id;
        this.nombre         = nombre;
        this.estado         = EstadoAgente.DISPONIBLE;
        this.unidadPolicial = unidadPolicial;
    }
 
    /** ¿El agente puede recibir una asignación? */
    public boolean estaDisponible() {
        return EstadoAgente.DISPONIBLE.equals(this.estado);
    }
 
    /** Marca el agente como ocupado al recibir una asignación. */
    public void asignar() {
        if (!estaDisponible())
            throw new IllegalStateException("El agente " + nombre + " no está disponible.");
        this.estado = EstadoAgente.OCUPADO;
    }
 
    /** Libera al agente cuando termina la asignación. */
    public void liberar() {
        this.estado = EstadoAgente.DISPONIBLE;
    }
}
 
