/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.port.in.CrearReporteHallazgosPort;
import com.callsos.backend.domain.port.out.AgenteByIdRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
 
import java.util.UUID;
 
/**
 * Caso de uso: el agente finaliza la atención y envía su reporte de hallazgos.
 *
 * Regla de negocio (clase ternaria):
 *   Sin Incidente + Agente → no existe ReporteHallazgos.
 *   Ambas referencias se validan antes de crear el reporte.
 *
 * Efecto colateral: el incidente transiciona a FINALIZADO.
 */
public class CrearReporteHallazgosService implements CrearReporteHallazgosPort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final AgenteByIdRepositoryPort agenteRepository;
    private final ReporteHallazgosRepositoryPort reporteRepository;
 
    public CrearReporteHallazgosService(
            IncidenteRepositoryPort incidenteRepository,
            AgenteByIdRepositoryPort agenteRepository,
            ReporteHallazgosRepositoryPort reporteRepository) {
        this.incidenteRepository = incidenteRepository;
        this.agenteRepository    = agenteRepository;
        this.reporteRepository   = reporteRepository;
    }
 
    @Override
    public ReporteHallazgos ejecutar(String incidenteId, String agenteId, String descripcion) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        Agente agente = agenteRepository
            .buscarPorId(agenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Agente no encontrado: " + agenteId));
 
        // Validar que el incidente esté en estado atendible
        if (!incidente.getEstado().name().equals("EN_ATENCION"))
            throw new IllegalStateException(
                "Solo se puede reportar sobre un incidente EN_ATENCION. " +
                "Estado actual: " + incidente.getEstado());
 
        ReporteHallazgos reporte = new ReporteHallazgos(
            UUID.randomUUID().toString(),
            descripcion,
            incidente,
            agente
        );
 
        reporteRepository.guardar(reporte);
 
        // Finalizar el incidente tras el reporte
        incidente.finalizar();
        incidenteRepository.guardar(incidente);
 
        return reporte;
    }
}
