package com.callsos.backend.infrastructure.adapter.in.web.dto;

import com.callsos.backend.domain.enums.TipoIncidente;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para actualizar el tipo de un incidente.
 * Llega en el body del PATCH /api/v1/incidentes/{id}/tipo.
 * El dominio nunca ve esta clase.
 */
public class ActualizarTipoIncidenteRequest {

    @NotNull(message = "El nuevo tipo de incidente es obligatorio")
    private TipoIncidente nuevoTipo;

    public TipoIncidente getNuevoTipo() { return nuevoTipo; }
}
