/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.valueobject.Ubicacion;

/**
 * Puerto de entrada: contrato para crear un nuevo incidente.
 *
 * El adaptador de entrada (REST controller) depende de esta interfaz,
 * nunca de la implementación concreta (CrearIncidenteService).
 * Eso garantiza el aislamiento hexagonal.
 */
public interface CrearIncidentePort {
    
    /**
     * @param denuncianteId  ID del denunciante que reporta
     * @param tipo           Tipo de incidente
     * @param descripcion    Descripción del hecho
     * @param ubicacion      Ubicación del incidente
     * @return               Incidente creado y persistido
     */
    Incidente ejecutar(String denuncianteId, TipoIncidente tipo,
                       String descripcion, Ubicacion ubicacion);
}
