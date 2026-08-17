/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.AtenderIncidentePort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: el agente llega al lugar y comienza la atención activa.
 *
 * Transición: AGENTE_EN_CAMINO → EN_ATENCION
 *
 * * FIX: se usaba EN_PROCESO que fue renombrado a EN_ATENCION en Fase 1
 * en la Fase 1. Ahora se delega al método semántico incidente.iniciarAtencion()
 * en lugar de cambiarEstado() con un enum hardcodeado.
 */
public class AtenderIncidenteService implements AtenderIncidentePort {
 
    private final IncidenteRepositoryPort incidenteRepository;
    private final EventPublisherPort eventPublisher;
 
    public AtenderIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                   EventPublisherPort eventPublisher) {
        this.incidenteRepository = incidenteRepository;
        this.eventPublisher      = eventPublisher;
    }
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        EstadoIncidente estadoAnterior = incidente.getEstado();

        // Usa el método semántico del agregado — él conoce la transición válida
        incidente.iniciarAtencion();
 
        incidenteRepository.guardar(incidente);

        // Épica 2 (fix P4): antes esta transición no quedaba auditada.
        eventPublisher.publicar(new IncidenteEvent(
            incidenteId, incidente.getDenunciante().getId(),
            estadoAnterior, EstadoIncidente.EN_ATENCION));
    }
}