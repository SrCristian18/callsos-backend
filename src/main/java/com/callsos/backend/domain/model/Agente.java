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
import com.callsos.backend.domain.valueobject.Ubicacion;
 
/**
 * Hoja del patrón Composite.
 *
 * Agente es el nivel más bajo de la jerarquía: no puede contener
 * subordinados. Hereda de AutoridadPolicial para participar
 * uniformemente en la estructura Composite.
 *
 * Además modela el estado operativo del agente (DISPONIBLE/OCUPADO)
 * y expone los métodos de negocio estaDisponible(), asignar() y liberar().
 */
public class Agente extends AutoridadPolicial{
 
    private EstadoAgente estado;
 
    public Agente(String id, String nombre, String direccion,
                  Ubicacion ubicacion, String telefono) {
        super(id, nombre, direccion, ubicacion, telefono);
        this.estado = EstadoAgente.DISPONIBLE;
    }
    
    // ── Composite: hoja no admite hijos ───────────────────────────────────
 
    @Override
    public void agregar(AutoridadPolicial componente) {
        throw new UnsupportedOperationException(
            "Un Agente es una hoja del Composite y no puede tener subordinados.");
    }
    
    @Override
    public void eliminar(AutoridadPolicial componente) {
        throw new UnsupportedOperationException(
            "Un Agente es una hoja del Composite y no puede tener subordinados.");
    }
 
    @Override
    public String getRol() {
        return "AGENTE";
    }
    
    // ── Comportamiento de dominio ──────────────────────────────────────────
 
    public EstadoAgente getEstado() { return estado; }
    
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
 
