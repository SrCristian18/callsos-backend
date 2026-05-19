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
 
import java.util.UUID;

/**
 * Caso de uso: asignar un agente disponible a un incidente.
 *
 * Orquesta:
 *   1. Verificar que el incidente tiene una UnidadPolicial asignada.
 *   2. Buscar un agente disponible en esa unidad (puerto de salida).
 *   3. Crear la Asignacion (que internamente ocupa al agente).
 *   4. Registrar la asignación en el incidente.
 *   5. Persistir los cambios.
 *
 * La Asignacion requiere una Denuncia (regla ternaria). Se obtiene
 * del incidente, que ya la tiene vinculada.
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
    public void ejecutar(Incidente incidente) {
 
        UnidadPolicial unidad = incidente.getUnidadPolicial();
        if (unidad == null)
            throw new IllegalStateException(
                "El incidente no tiene una UnidadPolicial asignada.");
 
        Agente agente = agenteRepository
            .buscarAgenteDisponible(unidad.getId())
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
        incidenteRepository.guardar(incidente);
        agenteRepository.guardar(agente);
    }
}
