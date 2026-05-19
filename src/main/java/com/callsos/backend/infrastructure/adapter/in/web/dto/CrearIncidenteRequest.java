/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.TipoIncidente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para crear un incidente.
 * Es lo que llega en el body del POST /incidentes.
 * El dominio nunca ve esta clase.
 */
public class CrearIncidenteRequest {
    
    @NotBlank(message = "El ID del denunciante es obligatorio")
    private String denuncianteId;
 
    @NotNull(message = "El tipo de incidente es obligatorio")
    private TipoIncidente tipo;
 
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
 
    @NotNull(message = "La ubicación es obligatoria")
    private UbicacionDto ubicacion;
 
    public String getDenuncianteId()    { return denuncianteId; }
    public TipoIncidente getTipo()      { return tipo; }
    public String getDescripcion()      { return descripcion; }
    public UbicacionDto getUbicacion()  { return ubicacion; }
}
