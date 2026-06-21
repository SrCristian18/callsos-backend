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
import com.callsos.backend.domain.port.in.ConsultarIncidentesAsignadosPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
import java.util.List;
 
/** Caso de uso: incidentes activos asignados al agente autenticado. */
public class ConsultarIncidentesAsignadosService implements ConsultarIncidentesAsignadosPort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public ConsultarIncidentesAsignadosService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public List<Incidente> ejecutar(String agenteId) {
        return incidenteRepository.buscarAsignadosAlAgente(agenteId);
    }
}
