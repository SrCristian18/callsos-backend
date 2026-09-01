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
import com.callsos.backend.domain.event.AgenteEnCaminoEvent;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.MarcarAgenteEnCaminoPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: el agente confirma que va en camino al incidente.
 * Transición: AGENTE_ASIGNADO → AGENTE_EN_CAMINO
 *
 * Publica AgenteEnCaminoEvent para:
 *   - Notificar al denunciante vía Firebase FCM
 *   - Activar el canal WebSocket de tracking GPS
 *
 * FIX (Épica 8, hallazgo de seguridad #2): antes este servicio no
 * validaba OWNERSHIP — cualquier agente autenticado (rol AGENTE
 * válido, pero sin relación con el incidente) podía marcar como
 * "en camino" un incidente asignado a un colega. Ahora se carga la
 * Asignacion activa del incidente y se compara asignacion.getAgente()
 * contra el actorId del JWT antes de ejecutar la transición — mismo
 * patrón que ActualizarTipoIncidenteService.
 */
public class MarcarAgenteEnCaminoService implements MarcarAgenteEnCaminoPort{
    
     private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
    private final EventPublisherPort eventPublisher;
 
    public MarcarAgenteEnCaminoService(IncidenteRepositoryPort incidenteRepository,
                                       AsignacionRepositoryPort asignacionRepository,
                                       EventPublisherPort eventPublisher) {
        this.incidenteRepository  = incidenteRepository;
        this.asignacionRepository = asignacionRepository;
        this.eventPublisher       = eventPublisher;
    }
 
    @Override
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
 
        EstadoIncidente estadoAnterior = incidente.getEstado();

        incidente.marcarAgenteEnCamino();
        incidenteRepository.guardar(incidente);
 
        // Publicar evento — Firebase y WebSocket escucharán esto
        eventPublisher.publicar(new AgenteEnCaminoEvent(
            incidenteId,
            incidente.getDenunciante().getId(),
            estadoAnterior,
            asignacionActiva.getAgente().getId()
        ));
    }
}