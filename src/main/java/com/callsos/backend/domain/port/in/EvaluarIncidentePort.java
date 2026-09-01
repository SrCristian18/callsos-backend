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
    
    /**
     * @param incidenteId  ID del incidente a evaluar/finalizar
     * @param actorId      ID del agente autenticado (JWT) — debe coincidir
     *                     con el agente de la Asignacion activa del
     *                     incidente. Épica 8, hallazgo #2: sin esta
     *                     validación, cualquier agente autenticado podía
     *                     operar sobre el incidente de un colega.
     */
    void ejecutar(String incidenteId, String actorId);
}
