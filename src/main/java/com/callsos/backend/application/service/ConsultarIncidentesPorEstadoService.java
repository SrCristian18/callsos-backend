/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.ConsultarIncidentesPorEstadoPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;

import java.util.List;

/**
 * Caso de uso: listar incidentes por estado.
 *
 * FIX (validación end-to-end): resuelve el Gap 2 de deuda_backend.md —
 * Comando no tenía ningún endpoint para listar incidentes CREADO
 * pendientes de derivar.
 *
 * Sin restricción de actorId — Comando tiene visibilidad total del
 * sistema, a diferencia de los otros roles que solo ven sus propios
 * incidentes.
 */
public class ConsultarIncidentesPorEstadoService
        implements ConsultarIncidentesPorEstadoPort {

    private final IncidenteRepositoryPort incidenteRepository;

    public ConsultarIncidentesPorEstadoService(
            IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }

    @Override
    public List<Incidente> ejecutar(EstadoIncidente estado) {
        return incidenteRepository.buscarPorEstado(estado);
    }
}
