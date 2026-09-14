/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.mapper;

import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AuditoriaResponse;

import java.util.List;

/**
 * AUD-arquitectura: mapper explícito dominio → DTO para el endpoint de
 * auditoría — ver el Javadoc de AuditoriaResponse para el porqué.
 */
public class AuditoriaMapper {

    private AuditoriaMapper() {}

    public static AuditoriaResponse toResponse(AuditoriaIncidente auditoria) {
        return new AuditoriaResponse(
            auditoria.getIncidenteId(),
            auditoria.getEstadoAnterior(),
            auditoria.getEstadoNuevo(),
            auditoria.getActorId(),
            auditoria.getActorRol(),
            auditoria.getTimestamp(),
            auditoria.getDetalle(),
            auditoria.getCampo(),
            auditoria.getValorAnteriorGenerico(),
            auditoria.getValorNuevoGenerico()
        );
    }

    public static List<AuditoriaResponse> toResponseList(List<AuditoriaIncidente> auditorias) {
        return auditorias.stream().map(AuditoriaMapper::toResponse).toList();
    }
}