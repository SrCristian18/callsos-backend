/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.application.service.support.AgenteLiberador;
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
 *
 * ESTE es el flujo que realmente usa la app para finalizar (ver
 * ReporteHallazgosView — llama directo a POST /reportes/hallazgos y
 * NUNCA a PATCH /{id}/evaluar). Por eso el fix del agente que queda
 * OCUPADO para siempre vive acá con la misma prioridad que en
 * EvaluarIncidenteService — ver el docstring de AgenteLiberador.
 */
public class CrearReporteHallazgosService implements CrearReporteHallazgosPort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final AgenteByIdRepositoryPort agenteRepository;
    private final ReporteHallazgosRepositoryPort reporteRepository;
    private final AgenteLiberador agenteLiberador;
 
    public CrearReporteHallazgosService(
            IncidenteRepositoryPort incidenteRepository,
            AgenteByIdRepositoryPort agenteRepository,
            ReporteHallazgosRepositoryPort reporteRepository,
            AgenteLiberador agenteLiberador) {
        this.incidenteRepository = incidenteRepository;
        this.agenteRepository    = agenteRepository;
        this.reporteRepository   = reporteRepository;
        this.agenteLiberador     = agenteLiberador;
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
 
        // FIX: antes de este cambio, este era el punto exacto donde el
        // agente quedaba OCUPADO en BD para siempre — ver
        // AgenteLiberador para el detalle completo.
        agenteLiberador.liberarSiHayAsignacionActiva(incidenteId);

        return reporte;
    }
}