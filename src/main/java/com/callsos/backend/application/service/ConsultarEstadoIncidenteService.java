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
import com.callsos.backend.domain.port.in.ConsultarEstadoIncidentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: consultar el estado actual de un incidente.
 * Operación de solo lectura — no llama a guardar().
 */
public class ConsultarEstadoIncidenteService implements ConsultarEstadoIncidentePort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public ConsultarEstadoIncidenteService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
    
    @Override
    public EstadoIncidente ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        return incidente.getEstado();
    }
}
