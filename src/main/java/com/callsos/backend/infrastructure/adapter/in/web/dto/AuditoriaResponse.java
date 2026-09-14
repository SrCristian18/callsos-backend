/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import com.callsos.backend.domain.enums.EstadoIncidente;
import java.time.LocalDateTime;

/**
 * DTO de salida para GET /api/v1/auditoria/incidente/{id} (AUD-arquitectura).
 *
 * ANTES: AuditoriaController devolvía `List<AuditoriaIncidente>` — el
 * modelo de dominio directo, serializado tal cual por Jackson. Eso
 * convierte cualquier cambio futuro al agregado de dominio (renombrar un
 * campo, agregar uno interno) en un cambio de contrato de API sin que
 * nadie lo decida explícitamente — el mismo problema, en más pequeño,
 * que ya se había señalado para otros controllers (por eso existen
 * IncidenteResponse, EtaResponse, etc. en este mismo paquete). Se
 * mapea 1:1 con AuditoriaMapper — mismos campos, sin recorte de datos
 * (ninguno de los campos de AuditoriaIncidente es sensible), pero ahora
 * el contrato de API es explícito e independiente del modelo de dominio.
 */
public class AuditoriaResponse {

    private final String incidenteId;
    private final EstadoIncidente estadoAnterior;
    private final EstadoIncidente estadoNuevo;
    private final String actorId;
    private final String actorRol;
    private final LocalDateTime timestamp;
    private final String detalle;
    private final String campo;
    private final String valorAnteriorGenerico;
    private final String valorNuevoGenerico;

    public AuditoriaResponse(String incidenteId,
                             EstadoIncidente estadoAnterior,
                             EstadoIncidente estadoNuevo,
                             String actorId,
                             String actorRol,
                             LocalDateTime timestamp,
                             String detalle,
                             String campo,
                             String valorAnteriorGenerico,
                             String valorNuevoGenerico) {
        this.incidenteId           = incidenteId;
        this.estadoAnterior        = estadoAnterior;
        this.estadoNuevo           = estadoNuevo;
        this.actorId               = actorId;
        this.actorRol              = actorRol;
        this.timestamp             = timestamp;
        this.detalle               = detalle;
        this.campo                 = campo;
        this.valorAnteriorGenerico = valorAnteriorGenerico;
        this.valorNuevoGenerico    = valorNuevoGenerico;
    }

    public String getIncidenteId()             { return incidenteId; }
    public EstadoIncidente getEstadoAnterior()  { return estadoAnterior; }
    public EstadoIncidente getEstadoNuevo()     { return estadoNuevo; }
    public String getActorId()                  { return actorId; }
    public String getActorRol()                 { return actorRol; }
    public LocalDateTime getTimestamp()         { return timestamp; }
    public String getDetalle()                  { return detalle; }
    public String getCampo()                    { return campo; }
    public String getValorAnteriorGenerico()    { return valorAnteriorGenerico; }
    public String getValorNuevoGenerico()       { return valorNuevoGenerico; }
}
