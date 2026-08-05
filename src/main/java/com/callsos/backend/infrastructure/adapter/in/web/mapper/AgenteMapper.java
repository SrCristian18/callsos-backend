package com.callsos.backend.infrastructure.adapter.in.web.mapper;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AgenteDisponibleResponse;

import java.util.List;

/**
 * Mapper: Agente (dominio) -> AgenteDisponibleResponse (DTO de salida).
 */
public final class AgenteMapper {

    private AgenteMapper() {}

    public static AgenteDisponibleResponse toResponse(Agente agente) {
        return new AgenteDisponibleResponse(
            agente.getId(),
            agente.getNombre(),
            agente.getTelefono(),
            agente.getEstado()
        );
    }

    public static List<AgenteDisponibleResponse> toResponseList(List<Agente> agentes) {
        return agentes.stream()
            .map(AgenteMapper::toResponse)
            .toList();
    }
}
