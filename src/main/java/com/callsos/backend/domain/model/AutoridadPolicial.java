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
import lombok.Getter;
 
/**
 * Clase base para cualquier entidad policial con autoridad.
 * UnidadPolicial hereda de esta clase (diagrama 2).
 */
@Getter
public abstract class AutoridadPolicial {
 
    protected final String id;       // UUID
    protected String nombre;
    protected String direccion;
    protected Ubicacion ubicacion;
    protected String telefono;
 
    protected AutoridadPolicial(String id, String nombre,
                                String direccion, Ubicacion ubicacion,
                                String telefono) {
        this.id        = id;
        this.nombre    = nombre;
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.telefono  = telefono;
    }
}