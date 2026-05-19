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
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.AtenderIncidentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: marcar un incidente como EN_PROCESO.
 * Representa el momento en que el agente llega al lugar del hecho
 * y comienza la atención activa.
 *
 * Transición: ASIGNADO → EN_PROCESO
 * La validación de la transición la ejecuta el agregado Incidente.
 */
public class AtenderIncidenteService implements AtenderIncidentePort {
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public AtenderIncidenteService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        incidente.cambiarEstado(EstadoIncidente.EN_PROCESO);
 
        incidenteRepository.guardar(incidente);
    }
    
}
