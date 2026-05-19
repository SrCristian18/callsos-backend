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
import com.callsos.backend.domain.enums.TipoIncidente;

import java.time.LocalDateTime;

/**
 * DTO de salida. Es lo que el cliente recibe en el JSON de respuesta.
 * Solo expone los campos que el exterior necesita conocer —
 * nunca el modelo de dominio completo.
 */
public class IncidenteResponse {
    
    private final String id;
    private final LocalDateTime fechaHora;
    private final TipoIncidente tipo;
    private final String descripcion;
    private final EstadoIncidente estado;
    private final double latitud;
    private final double longitud;
    private final String denuncianteId;
 
    public IncidenteResponse(String id, LocalDateTime fechaHora, TipoIncidente tipo,
                             String descripcion, EstadoIncidente estado,
                             double latitud, double longitud, String denuncianteId) {
        this.id            = id;
        this.fechaHora     = fechaHora;
        this.tipo          = tipo;
        this.descripcion   = descripcion;
        this.estado        = estado;
        this.latitud       = latitud;
        this.longitud      = longitud;
        this.denuncianteId = denuncianteId;
    }
    
    public String getId()               { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public TipoIncidente getTipo()      { return tipo; }
    public String getDescripcion()      { return descripcion; }
    public EstadoIncidente getEstado()  { return estado; }
    public double getLatitud()          { return latitud; }
    public double getLongitud()         { return longitud; }
    public String getDenuncianteId()    { return denuncianteId; }
}
