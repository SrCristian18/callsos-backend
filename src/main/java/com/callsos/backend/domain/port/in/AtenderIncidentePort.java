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
     * @param actorId      ID del agente autenticado (JWT) — debe coincidir
     *                     con el agente de la Asignacion activa del
     *                     incidente. Épica 8, hallazgo #2: sin esta
     *                     validación, cualquier agente autenticado podía
     *                     operar sobre el incidente de un colega.
     */
    void ejecutar(String incidenteId, String actorId);
}