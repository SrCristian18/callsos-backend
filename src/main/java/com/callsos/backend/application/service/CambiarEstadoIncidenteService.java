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
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.CambiarEstadoIncidentePort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: cambiar el estado de un incidente existente.
 * La validación de la transición ocurre en el agregado Incidente.
 *
 * También es el camino que usa IncidenteController.cancelar() (con
 * nuevoEstado = CANCELADO) — publicar el evento acá cubre tanto
 * transiciones genéricas como la cancelación (Épica 2, fix P4).
 *
 * FIX: si el incidente tenía un agente asignado/en camino/en atención
 * y se cancela, ese agente quedaba OCUPADO en BD para siempre — mismo
 * bug que en EvaluarIncidenteService/CrearReporteHallazgosService, ver
 * AgenteLiberador. Solo se libera cuando nuevoEstado es terminal
 * (FINALIZADO o CANCELADO) — este método también se usa para
 * transiciones intermedias no terminales, donde el agente sigue
 * trabajando y NO debe liberarse.
 */
public class CambiarEstadoIncidenteService implements CambiarEstadoIncidentePort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final EventPublisherPort eventPublisher;
    private final AgenteLiberador agenteLiberador;
 
    public CambiarEstadoIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                         EventPublisherPort eventPublisher,
                                         AgenteLiberador agenteLiberador) {
        this.incidenteRepository = incidenteRepository;
        this.eventPublisher      = eventPublisher;
        this.agenteLiberador     = agenteLiberador;
    }
    
     @Override
    public void ejecutar(String incidenteId, EstadoIncidente nuevoEstado) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        EstadoIncidente estadoAnterior = incidente.getEstado();

        incidente.cambiarEstado(nuevoEstado);
 
        incidenteRepository.guardar(incidente);

        if (nuevoEstado == EstadoIncidente.FINALIZADO
                || nuevoEstado == EstadoIncidente.CANCELADO) {
            agenteLiberador.liberarSiHayAsignacionActiva(incidenteId);
        }

        eventPublisher.publicar(new IncidenteEvent(
            incidenteId, incidente.getDenunciante().getId(),
            estadoAnterior, incidente.getEstado()));
    }
}