/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.CrearIncidentePort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
 
import java.util.UUID;
 
/**
 * Caso de uso: crear un nuevo incidente a partir de una denuncia.
 *
 * Implementa CrearIncidentePort (puerto de entrada).
 * Depende de IncidenteRepositoryPort y DenuncianteRepositoryPort
 * (puertos de salida) — nunca de clases de infraestructura directamente.
 */
public class CrearIncidenteService implements CrearIncidentePort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final DenuncianteRepositoryPort denuncianteRepository;
 
    public CrearIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                 DenuncianteRepositoryPort denuncianteRepository) {
        this.incidenteRepository  = incidenteRepository;
        this.denuncianteRepository = denuncianteRepository;
    }
    
    @Override
    public Incidente ejecutar(String denuncianteId, TipoIncidente tipo,
                              String descripcion, Ubicacion ubicacion) {
 
        Denunciante denunciante = denuncianteRepository
            .buscarPorId(denuncianteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Denunciante no encontrado: " + denuncianteId));
 
        Incidente incidente = new Incidente(
            UUID.randomUUID().toString(),
            tipo,
            descripcion,
            ubicacion,
            denunciante
        );
 
        // guardar() es void — persistimos y retornamos el mismo objeto
        incidenteRepository.guardar(incidente);
        return incidente;
    }
}
