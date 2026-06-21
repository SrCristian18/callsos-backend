package com.callsos.backend.infrastructure.adapter.in.web.mapper;

import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.dto.UbicacionDto;

import java.util.List;

public class IncidenteMapper {

    private IncidenteMapper() {}

    public static Ubicacion toUbicacion(UbicacionDto dto) {
        return new Ubicacion(dto.getLatitud(), dto.getLongitud());
    }

    /** Incidente → DTO con CAI incluido si ya fue derivado. */
    public static IncidenteResponse toResponse(Incidente incidente) {
        String unidadId  = incidente.getUnidadPolicial() != null
            ? incidente.getUnidadPolicial().getId() : null;
        String nombreCAI = incidente.getUnidadPolicial() != null
            ? incidente.getUnidadPolicial().getNombre() : null;

        return new IncidenteResponse(
            incidente.getId(),
            incidente.getFechaHora(),
            incidente.getTipo(),
            incidente.getDescripcion(),
            incidente.getEstado(),
            incidente.getUbicacion().getLatitud(),
            incidente.getUbicacion().getLongitud(),
            incidente.getDenunciante().getId(),
            unidadId,
            nombreCAI
        );
    }

    /** Lista de incidentes → lista de DTOs. */
    public static List<IncidenteResponse> toResponseList(List<Incidente> incidentes) {
        return incidentes.stream()
            .map(IncidenteMapper::toResponse)
            .toList();
    }
}