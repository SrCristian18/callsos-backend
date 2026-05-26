/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.MarcarAgenteEnCaminoPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: el agente confirma que va en camino al incidente.
 *
 * Transición: AGENTE_ASIGNADO → AGENTE_EN_CAMINO
 * La validación de la transición la ejecuta el agregado Incidente.
 *
 * Efecto en Fase 2: este cambio de estado activa el canal WebSocket
 * de tracking GPS para el denunciante.
 */
public class MarcarAgenteEnCaminoService implements MarcarAgenteEnCaminoPort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public MarcarAgenteEnCaminoService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        // Delega la transición al agregado — él valida que sea AGENTE_ASIGNADO
        incidente.marcarAgenteEnCamino();
 
        incidenteRepository.guardar(incidente);
    }
}
