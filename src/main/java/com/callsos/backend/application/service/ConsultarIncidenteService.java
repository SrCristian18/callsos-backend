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
import com.callsos.backend.domain.port.in.ConsultarIncidentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/** Caso de uso: obtener el detalle completo de un incidente por ID. */
public class ConsultarIncidenteService implements ConsultarIncidentePort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public ConsultarIncidenteService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public Incidente ejecutar(String incidenteId) {
        return incidenteRepository.buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
    }
    
}
