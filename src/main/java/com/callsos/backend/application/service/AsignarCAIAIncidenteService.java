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
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.in.AsignarCAIAIncidentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
 
/**
 * Caso de uso: el Comando deriva el incidente al CAI más cercano.
 *
 * Orquesta:
 *   1. Cargar el incidente por ID.
 *   2. Buscar la UnidadPolicial más cercana a la ubicación del incidente
 *      (Haversine en BD vía UnidadPolicialRepositoryPort).
 *   3. Llamar a incidente.derivarACAI(unidad) — transición de estado
 *      CREADO → DERIVADO_A_CAI gestionada por el agregado.
 *   4. Persistir el incidente actualizado.
 *
 * Este es el paso que faltaba entre CrearIncidente y AsignarAgente.
 * Sin él, AsignarAgenteService siempre fallaba con
 * "El incidente no tiene una UnidadPolicial asignada".
 */
public class AsignarCAIAIncidenteService implements AsignarCAIAIncidentePort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final UnidadPolicialRepositoryPort unidadPolicialRepository;
 
    public AsignarCAIAIncidenteService(
            IncidenteRepositoryPort incidenteRepository,
            UnidadPolicialRepositoryPort unidadPolicialRepository) {
        this.incidenteRepository    = incidenteRepository;
        this.unidadPolicialRepository = unidadPolicialRepository;
    }
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        UnidadPolicial cai = unidadPolicialRepository
            .buscarPorUbicacion(incidente.getUbicacion())
            .orElseThrow(() -> new IllegalStateException(
                "No se encontró ningún CAI disponible para la ubicación del incidente."));
 
        // Transición CREADO → DERIVADO_A_CAI (validada por el agregado)
        incidente.derivarACAI(cai);
 
        // Guardar persiste la unidad_policial_id y el nuevo estado
        incidenteRepository.guardar(incidente);
    }
}
