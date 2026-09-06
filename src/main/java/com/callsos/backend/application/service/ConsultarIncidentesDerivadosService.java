/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.ConsultarIncidentesDerivadosPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;

import java.util.List;

/**
 * Caso de uso: historial completo de derivaciones, para el tab
 * "Delegados" de COMANDO.
 *
 * EPIC-18 (frontend) / hallazgo #14: ver el comentario de
 * {@link ConsultarIncidentesDerivadosPort} y de
 * {@link IncidenteRepositoryPort#buscarDerivados()} para el porqué del
 * criterio de filtrado (unidad asignada, no estado).
 */
public class ConsultarIncidentesDerivadosService
        implements ConsultarIncidentesDerivadosPort {

    private final IncidenteRepositoryPort incidenteRepository;

    public ConsultarIncidentesDerivadosService(
            IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }

    @Override
    public List<Incidente> ejecutar() {
        return incidenteRepository.buscarDerivados();
    }
}
