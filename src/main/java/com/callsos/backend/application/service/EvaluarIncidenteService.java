/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */


import com.callsos.backend.application.service.support.AgenteLiberador;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.event.IncidenteFinalizadoEvent;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.EvaluarIncidentePort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: el agente finaliza la atención.
 * Transición: EN_ATENCION → FINALIZADO
 * Publica IncidenteFinalizadoEvent para notificar al denunciante.
 *
 * FIX: antes esta transición dejaba al agente OCUPADO en BD para
 * siempre — ver el docstring de AgenteLiberador para el detalle
 * completo del bug.
 */
public class EvaluarIncidenteService implements EvaluarIncidentePort {
 
     private final IncidenteRepositoryPort incidenteRepository;
    private final EventPublisherPort eventPublisher;
    private final AgenteLiberador agenteLiberador;
 
    public EvaluarIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                   EventPublisherPort eventPublisher,
                                   AgenteLiberador agenteLiberador) {
        this.incidenteRepository = incidenteRepository;
        this.eventPublisher      = eventPublisher;
        this.agenteLiberador     = agenteLiberador;
    }
 
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        if (!EstadoIncidente.EN_ATENCION.equals(incidente.getEstado()))
            throw new IllegalStateException(
                "Solo se puede evaluar un incidente EN_ATENCION. Estado actual: "
                + incidente.getEstado());
 
        EstadoIncidente estadoAnterior = incidente.getEstado();

        incidente.finalizar();
        incidenteRepository.guardar(incidente);
 
        agenteLiberador.liberarSiHayAsignacionActiva(incidenteId);

        eventPublisher.publicar(new IncidenteFinalizadoEvent(
            incidenteId,
            incidente.getDenunciante().getId(),
            estadoAnterior,
            EstadoIncidente.FINALIZADO
        ));
    }
}