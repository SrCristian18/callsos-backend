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
 * Puerto de entrada: contrato para cambiar el estado de un incidente.
 */
public interface CambiarEstadoIncidentePort {
    /**
     * @param incidenteId  ID del incidente a modificar
     * @param nuevoEstado  Estado al que se desea transicionar
     */
    void ejecutar(String incidenteId, EstadoIncidente nuevoEstado);
}
