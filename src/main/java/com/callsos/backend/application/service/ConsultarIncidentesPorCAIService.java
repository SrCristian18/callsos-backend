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
import com.callsos.backend.domain.port.in.ConsultarIncidentesPorCAIPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
import java.util.List;
 
/** Caso de uso: incidentes activos de una unidad policial para el panel del CAI. */
public class ConsultarIncidentesPorCAIService implements ConsultarIncidentesPorCAIPort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public ConsultarIncidentesPorCAIService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public List<Incidente> ejecutar(String unidadPolicialId) {
        return incidenteRepository.buscarPorCAI(unidadPolicialId);
    }
}
