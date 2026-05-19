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
 * Puerto de entrada: contrato para evaluar y finalizar un incidente.
 * Recibe el ID del incidente — el caso de uso se encarga de cargarlo.
 */
public interface EvaluarIncidentePort {
    
    void ejecutar(String incidenteId);
}
