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
 *
 * Épica 7: se agregan agenteId y nombreAgente — CAI y Comando los
 * necesitan para saber a qué topic de tracking suscribirse
 * (/topic/agente/{agenteId}/ubicacion, ver Épica 3); antes de esta
 * épica no había forma de que el frontend supiera qué agente estaba
 * asignado a un incidente sin una llamada aparte. Ambos null si el
 * incidente todavía no tiene una asignación ACTIVA (ver
 * IncidenteMapper.toResponse(Incidente, Asignacion)).
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
    private final String agenteId;         // null si aún no hay asignación activa
    private final String nombreAgente;     // null si aún no hay asignación activa

    public IncidenteResponse(String id, LocalDateTime fechaHora, TipoIncidente tipo,
                             String descripcion, EstadoIncidente estado,
                             double latitud, double longitud,
                             String denuncianteId,
                             String unidadPolicialId, String nombreCAI,
                             String agenteId, String nombreAgente) {
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
        this.agenteId         = agenteId;
        this.nombreAgente     = nombreAgente;
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
    public String getAgenteId()          { return agenteId; }
    public String getNombreAgente()      { return nombreAgente; }
}