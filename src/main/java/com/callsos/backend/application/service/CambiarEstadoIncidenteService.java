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
import com.callsos.backend.domain.port.in.CambiarEstadoIncidentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: cambiar el estado de un incidente existente.
 * La validación de la transición ocurre en el agregado Incidente.
 */
public class CambiarEstadoIncidenteService implements CambiarEstadoIncidentePort {
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public CambiarEstadoIncidenteService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
    
     @Override
    public void ejecutar(String incidenteId, EstadoIncidente nuevoEstado) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        incidente.cambiarEstado(nuevoEstado);
 
        incidenteRepository.guardar(incidente);
    }
}
