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
import com.callsos.backend.domain.port.in.ConsultarMisIncidentesPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
import java.util.List;
 
/** Caso de uso: historial de incidentes del denunciante autenticado. */
public class ConsultarMisIncidentesService implements ConsultarMisIncidentesPort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public ConsultarMisIncidentesService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public List<Incidente> ejecutar(String denuncianteId) {
        return incidenteRepository.buscarPorDenunciante(denuncianteId);
    }
}
