/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
 
/**
 * Puerto de entrada: contrato para consultar el estado actual de un incidente.
 * Operación de solo lectura — no produce efectos secundarios.
 */
public interface ConsultarEstadoIncidentePort {
    
    /**
     * @param incidenteId  ID del incidente a consultar
     * @return             Estado actual del incidente
     */
    EstadoIncidente ejecutar(String incidenteId);
}
