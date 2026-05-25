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
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.in.AsignarAgentePort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: asignar un agente disponible a un incidente.
 *
 * PRECONDICIÓN: el incidente debe estar en estado DERIVADO_A_CAI
 * (AsignarCAIAIncidenteService debe haberse ejecutado antes).
 *
 * BUG CORREGIDO:
 *   ANTES: unidad.getSubordinados().contains(a)
 *   → Comparaba referencias en memoria. Los Agente de BD son instancias
 *     nuevas, distintas a las de la lista de subordinados (vacía en BD).
 *     equals() no estaba sobrescrito → siempre false → nunca asignaba.
 *
 *   AHORA: filtra por unidad_policial_id en la consulta SQL directamente,
 *   usando AgenteRepositoryPort.obtenerDisponiblesPorUnidad(unidadId).
 *   La comparación es por ID de BD, no por referencia de objeto Java.
 */
public class AsignarAgenteService implements AsignarAgentePort {
    
    private final AgenteRepositoryPort agenteRepository;
    private final IncidenteRepositoryPort incidenteRepository;
 
    public AsignarAgenteService(AgenteRepositoryPort agenteRepository,
                                IncidenteRepositoryPort incidenteRepository) {
        this.agenteRepository    = agenteRepository;
        this.incidenteRepository = incidenteRepository;
    }
    
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        UnidadPolicial unidad = incidente.getUnidadPolicial();
        if (unidad == null)
            throw new IllegalStateException(
                "El incidente no tiene CAI asignado. " +
                "Ejecute AsignarCAIAIncidente antes de asignar un agente.");
 
         // FIX: consulta SQL filtra por unidad_policial_id → comparación por ID, no por referencia
        List<Agente> disponibles = agenteRepository
            .obtenerDisponiblesPorUnidad(unidad.getId());
 
        Agente agente = disponibles.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No hay agentes disponibles en la unidad: " + unidad.getNombre()));
 
        Denuncia denuncia = incidente.getDenuncia();
        if (denuncia == null)
            throw new IllegalStateException(
                "El incidente no tiene una Denuncia vinculada.");
 
        Asignacion asignacion = new Asignacion(
            UUID.randomUUID().toString(),
            agente,
            denuncia
        );
 
        incidente.agregarAsignacion(asignacion);
        incidente.marcarAgenteAsignado();
 
        incidenteRepository.guardar(incidente);
        agenteRepository.actualizarEstado(agente);
    }
}
