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
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.EvaluarIncidentePort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import org.springframework.transaction.annotation.Transactional;
 
/**
 * Caso de uso: el agente finaliza la atención.
 * Transición: EN_ATENCION → FINALIZADO
 * Publica IncidenteFinalizadoEvent para notificar al denunciante.
 *
 * FIX: antes esta transición dejaba al agente OCUPADO en BD para
 * siempre — ver el docstring de AgenteLiberador para el detalle
 * completo del bug.
 *
 * FIX (Épica 8, hallazgo de seguridad #2): antes este servicio no
 * validaba OWNERSHIP — cualquier agente autenticado (rol AGENTE
 * válido, pero sin relación con el incidente) podía finalizar un
 * incidente asignado a un colega. Ahora se carga la Asignacion activa
 * del incidente y se compara asignacion.getAgente() contra el actorId
 * del JWT antes de ejecutar la transición — mismo patrón que
 * ActualizarTipoIncidenteService.
 */
public class EvaluarIncidenteService implements EvaluarIncidentePort {
 
     private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
    private final EventPublisherPort eventPublisher;
    private final AgenteLiberador agenteLiberador;
 
    public EvaluarIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                   AsignacionRepositoryPort asignacionRepository,
                                   EventPublisherPort eventPublisher,
                                   AgenteLiberador agenteLiberador) {
        this.incidenteRepository  = incidenteRepository;
        this.asignacionRepository = asignacionRepository;
        this.eventPublisher       = eventPublisher;
        this.agenteLiberador      = agenteLiberador;
    }
 
 
    @Override
    @Transactional
    // FIX (Épica 8): incidente.guardar() + (vía AgenteLiberador)
    // asignacion.guardar() + agente.actualizarEstado() eran 3 escrituras
    // sin protección transaccional.
    public void ejecutar(String incidenteId, String actorId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));

        // Ownership: solo el agente REALMENTE asignado puede operar
        // sobre este incidente (Épica 8, hallazgo #2).
        Asignacion asignacionActiva = asignacionRepository
            .buscarPorIncidente(incidenteId)
            .orElseThrow(() -> new AccesoDenegadoException(
                "No hay una asignación activa para este incidente."));

        if (!asignacionActiva.getAgente().getId().equals(actorId)) {
            throw new AccesoDenegadoException(
                "El agente autenticado no es el agente asignado a este incidente.");
        }
 
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