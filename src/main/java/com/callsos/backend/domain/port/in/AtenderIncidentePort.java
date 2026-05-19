/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

/**
 * Puerto de entrada: contrato para marcar un incidente como EN_PROCESO.
 * Representa el momento en que el agente llega al lugar del hecho.
 */
public interface AtenderIncidentePort {
    
    /**
     * @param incidenteId  ID del incidente que pasa a ser atendido
     */
    void ejecutar(String incidenteId);
}
