/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import jakarta.validation.constraints.NotNull;


/**
 * DTO de entrada para cambiar el estado de un incidente.
 * Llega en el body del PATCH /incidentes/{id}/estado.
 */
public class CambiarEstadoRequest {
    
    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoIncidente nuevoEstado;
 
    public EstadoIncidente getNuevoEstado() { return nuevoEstado; }
}
