/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.AutoridadPolicial;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteAdministrativo;
import com.callsos.backend.domain.port.in.CrearReporteAdministrativoPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteAdministrativoRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
 
import java.util.UUID;
 
/**
 * Caso de uso: el Comando o CAI genera un reporte administrativo del incidente.
 *
 * Se usa en dos momentos del flujo:
 *   - Paso 3: el Comando genera el reporte al derivar el incidente al CAI.
 *   - Paso 11: el CAI reenvía el reporte de hallazgos al Comando.
 *
 * Regla de negocio (clase ternaria):
 *   Sin Incidente + AutoridadPolicial → no existe ReporteAdministrativo.
 */
public class CrearReporteAdministrativoService implements CrearReporteAdministrativoPort {
    
     private final IncidenteRepositoryPort incidenteRepository;
    private final UnidadPolicialRepositoryPort unidadPolicialRepository;
    private final ReporteAdministrativoRepositoryPort reporteRepository;
 
    public CrearReporteAdministrativoService(
            IncidenteRepositoryPort incidenteRepository,
            UnidadPolicialRepositoryPort unidadPolicialRepository,
            ReporteAdministrativoRepositoryPort reporteRepository) {
        this.incidenteRepository    = incidenteRepository;
        this.unidadPolicialRepository = unidadPolicialRepository;
        this.reporteRepository      = reporteRepository;
    }
 
    @Override
    public ReporteAdministrativo ejecutar(String incidenteId,
                                          String autoridadId,
                                          String resumen) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        // Buscamos la unidad policial como autoridad generadora del reporte
        AutoridadPolicial autoridad = unidadPolicialRepository
            .buscarPorId(autoridadId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unidad policial no encontrada: " + autoridadId));
 
        ReporteAdministrativo reporte = new ReporteAdministrativo(
            UUID.randomUUID().toString(),
            resumen,
            incidente,
            autoridad
        );
 
        reporteRepository.guardar(reporte);
        return reporte;
    }
}
