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
import com.callsos.backend.domain.port.in.EvaluarIncidentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
 
/**
 * Caso de uso: evaluar y finalizar un incidente atendido.
 * Transición: EN_PROCESO → FINALIZADO.
 */
public class EvaluarIncidenteService implements EvaluarIncidentePort{
    
    private final IncidenteRepositoryPort incidenteRepository;
 
    public EvaluarIncidenteService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
    }
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        if (!EstadoIncidente.EN_PROCESO.equals(incidente.getEstado()))
            throw new IllegalStateException(
                "Solo se puede evaluar un incidente EN_PROCESO. Estado actual: "
                + incidente.getEstado());
 
        incidente.cambiarEstado(EstadoIncidente.FINALIZADO);
        incidenteRepository.guardar(incidente);
    }
}
