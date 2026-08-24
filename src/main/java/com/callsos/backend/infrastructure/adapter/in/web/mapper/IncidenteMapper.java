package com.callsos.backend.infrastructure.adapter.in.web.mapper;

import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.dto.UbicacionDto;

import java.util.List;
import java.util.Optional;

public class IncidenteMapper {

    private IncidenteMapper() {}

    public static Ubicacion toUbicacion(UbicacionDto dto) {
        return new Ubicacion(dto.getLatitud(), dto.getLongitud());
    }

    /**
     * Incidente → DTO con CAI incluido si ya fue derivado, sin agente
     * (agenteId/nombreAgente quedan null). Usado en las listas
     * (mis-incidentes, asignados, por-cai, por-estado) donde resolver la
     * asignación activa de CADA fila implicaría una consulta extra por
     * incidente — hoy ninguna pantalla de lista necesita ese dato, solo
     * el detalle individual (ver toResponse(Incidente, Asignacion)).
     */
    public static IncidenteResponse toResponse(Incidente incidente) {
        return toResponse(incidente, null);
    }

    /**
     * Épica 7: variante con la Asignacion activa ya resuelta por el
     * caller (IncidenteController.consultar(), la única consulta de UN
     * incidente puntual — desde ahí es de donde Flutter navega a
     * TrackingView) — agenteId/nombreAgente quedan null si aún no hay
     * asignación (asignacionActiva == null), sin necesidad de que el
     * caller distinga ese caso.
     */
    public static IncidenteResponse toResponse(Incidente incidente, Asignacion asignacionActiva) {
        String unidadId  = incidente.getUnidadPolicial() != null
            ? incidente.getUnidadPolicial().getId() : null;
        String nombreCAI = incidente.getUnidadPolicial() != null
            ? incidente.getUnidadPolicial().getNombre() : null;

        String agenteId     = asignacionActiva != null
            ? asignacionActiva.getAgente().getId() : null;
        String nombreAgente = asignacionActiva != null
            ? asignacionActiva.getAgente().getNombre() : null;

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
            nombreCAI,
            agenteId,
            nombreAgente
        );
    }

    /** Lista de incidentes → lista de DTOs (sin agente, ver toResponse(Incidente)). */
    public static List<IncidenteResponse> toResponseList(List<Incidente> incidentes) {
        return incidentes.stream()
            .map(IncidenteMapper::toResponse)
            .toList();
    }
}