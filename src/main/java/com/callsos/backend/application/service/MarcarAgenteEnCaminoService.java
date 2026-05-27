/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.event.AgenteEnCaminoEvent;
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
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        incidente.marcarAgenteEnCamino();
        incidenteRepository.guardar(incidente);
 
        // Obtener el ID del agente asignado desde la asignación activa
        String agenteId = asignacionRepository
            .buscarPorIncidente(incidenteId)
            .map(a -> a.getAgente().getId())
            .orElse("desconocido");
 
        // Publicar evento — Firebase y WebSocket escucharán esto
        eventPublisher.publicar(new AgenteEnCaminoEvent(
            incidenteId,
            incidente.getDenunciante().getId(),
            agenteId
        ));
    }
}
