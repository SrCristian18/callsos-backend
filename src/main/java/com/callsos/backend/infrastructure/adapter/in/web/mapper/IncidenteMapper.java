/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.mapper;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.dto.UbicacionDto;

/**
 * Mapper del adaptador de entrada.
 *
 * Responsabilidad única: traducir entre el mundo HTTP (DTOs)
 * y el mundo del dominio (modelos, value objects).
 *
 * No usa librerías externas (MapStruct, ModelMapper) para mantener
 * la lógica de mapeo explícita y fácil de auditar.
 */
public class IncidenteMapper {
    
    private IncidenteMapper() {}
 
    /**
     * DTO de ubicación  →  Value object de dominio.
     */
    public static Ubicacion toUbicacion(UbicacionDto dto) {
        return new Ubicacion(dto.getLatitud(), dto.getLongitud());
    }
    
    /**
     * Modelo de dominio  →  DTO de respuesta HTTP.
     * Solo se exponen los campos que el cliente necesita.
     */
    public static IncidenteResponse toResponse(Incidente incidente) {
        return new IncidenteResponse(
            incidente.getId(),
            incidente.getFechaHora(),
            incidente.getTipo(),
            incidente.getDescripcion(),
            incidente.getEstado(),
            incidente.getUbicacion().getLatitud(),
            incidente.getUbicacion().getLongitud(),
            incidente.getDenunciante().getId()
        );
    }
}
