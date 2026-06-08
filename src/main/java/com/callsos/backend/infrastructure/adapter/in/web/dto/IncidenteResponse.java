package com.callsos.backend.infrastructure.adapter.in.web.dto;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;

import java.time.LocalDateTime;

/**
 * DTO de salida para Incidente.
 *
 * Fase E: se agregan unidadPolicialId y nombreCAI para que Flutter
 * pueda mostrar en la pantalla de detalle qué CAI está atendiendo
 * el incidente, sin necesidad de hacer una segunda llamada.
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
    private final String unidadPolicialId; // null si aún no derivado al CAI
    private final String nombreCAI;        // null si aún no derivado al CAI

    public IncidenteResponse(String id, LocalDateTime fechaHora, TipoIncidente tipo,
                             String descripcion, EstadoIncidente estado,
                             double latitud, double longitud,
                             String denuncianteId,
                             String unidadPolicialId, String nombreCAI) {
        this.id               = id;
        this.fechaHora        = fechaHora;
        this.tipo             = tipo;
        this.descripcion      = descripcion;
        this.estado           = estado;
        this.latitud          = latitud;
        this.longitud         = longitud;
        this.denuncianteId    = denuncianteId;
        this.unidadPolicialId = unidadPolicialId;
        this.nombreCAI        = nombreCAI;
    }

    public String getId()                { return id; }
    public LocalDateTime getFechaHora()  { return fechaHora; }
    public TipoIncidente getTipo()       { return tipo; }
    public String getDescripcion()       { return descripcion; }
    public EstadoIncidente getEstado()   { return estado; }
    public double getLatitud()           { return latitud; }
    public double getLongitud()          { return longitud; }
    public String getDenuncianteId()     { return denuncianteId; }
    public String getUnidadPolicialId()  { return unidadPolicialId; }
    public String getNombreCAI()         { return nombreCAI; }
}