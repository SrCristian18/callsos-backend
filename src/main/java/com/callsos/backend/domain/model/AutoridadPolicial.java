/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.valueobject.Ubicacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Componente base del patrón Composite.
 *
 * Tanto Agente (hoja) como UnidadPolicial (nodo compuesto)
 * heredan de esta clase. AutoridadPolicial mantiene una lista
 * de sus componentes hijo, lo que permite construir jerarquías
 * arbitrarias: una UnidadPolicial puede contener Agentes u otras
 * UnidadesPolicial.
 *
 * La agregación (rombo blanco) del diagrama 2 se representa con
 * la lista `subordinados` — AutoridadPolicial agrega instancias
 * de sí misma.
 */

public abstract class AutoridadPolicial {
 
    protected final String id;       // UUID
    protected String nombre;
    protected String direccion;
    protected Ubicacion ubicacion;
    protected String telefono;
    
    /** Lista de componentes hijo (Composite). */
    private final List<AutoridadPolicial> subordinados;
 
    protected AutoridadPolicial(String id, String nombre,
                                String direccion, Ubicacion ubicacion,
                                String telefono) {
        this.id        = id;
        this.nombre    = nombre;
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.telefono  = telefono;
        this.subordinados = new ArrayList<>();
    }
    
     // ── Operaciones Composite ──────────────────────────────────────────────
 
    /**
     * Agrega un subordinado (Agente u otra UnidadPolicial).
     * Las hojas (Agente) sobreescriben este método lanzando excepción
     * porque no pueden tener hijos.
     */
    public void agregar(AutoridadPolicial componente) {
        if (componente == null)
            throw new IllegalArgumentException("El componente no puede ser nulo.");
        subordinados.add(componente);
    }
    
    /**
     * Elimina un subordinado del nodo.
     */
    public void eliminar(AutoridadPolicial componente) {
        subordinados.remove(componente);
    }
    
    /** Vista inmutable de los subordinados. */
    public List<AutoridadPolicial> getSubordinados() {
        return Collections.unmodifiableList(subordinados);
    }
    
    // ── Getters ────────────────────────────────────────────────────────────
 
    public String getId()          { return id; }
    public String getNombre()      { return nombre; }
    public String getDireccion()   { return direccion; }
    public Ubicacion getUbicacion(){ return ubicacion; }
    public String getTelefono()    { return telefono; }
 
    /**
     * Operación polimórfica del Composite.
     * Cada subclase describe su rol en la jerarquía.
     */
    public abstract String getRol();
}