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
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: asignar un agente disponible a un incidente.
 *
 * FIX 1 (contains → SQL): el filtro de agentes ahora se hace en BD
 *   por unidad_policial_id, no en memoria por referencia de objeto.
 *
 * FIX 2 (AsignacionRepositoryPort): la Asignacion ahora se persiste
 *   en BD a través del puerto de salida correspondiente.
 *   Antes se creaba en memoria pero nunca se guardaba en la tabla asignaciones.
 */
public class AsignarAgenteService implements AsignarAgentePort {
 
    private final AgenteRepositoryPort agenteRepository;
    private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
 
    public AsignarAgenteService(AgenteRepositoryPort agenteRepository,
                                IncidenteRepositoryPort incidenteRepository,
                                AsignacionRepositoryPort asignacionRepository) {
        this.agenteRepository      = agenteRepository;
        this.incidenteRepository   = incidenteRepository;
        this.asignacionRepository  = asignacionRepository;
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
                "Ejecute /derivar antes de /asignar.");
 
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
 
        // Persistir los tres objetos afectados
        asignacionRepository.guardar(asignacion);   // FIX: antes no se guardaba
        incidenteRepository.guardar(incidente);
        agenteRepository.actualizarEstado(agente);
    }
}