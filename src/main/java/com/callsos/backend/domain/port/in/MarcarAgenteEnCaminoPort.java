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
 * Puerto de entrada: el agente acepta el incidente y sale hacia el lugar.
 *
 * Transición: AGENTE_ASIGNADO → AGENTE_EN_CAMINO
 *
 * Este estado es el puente entre el flujo de negocio (Fase 1) y el
 * tracking en tiempo real (Fase 2): una vez que el incidente está
 * AGENTE_EN_CAMINO, el WebSocket empieza a transmitir la posición GPS.
 *
 * Sin este puerto, el flujo saltaba de AGENTE_ASIGNADO directamente a
 * EN_ATENCION, imposibilitando el tracking.
 */
public interface MarcarAgenteEnCaminoPort {
    
    /**
     * @param incidenteId  ID del incidente que el agente va a atender
     */
    void ejecutar(String incidenteId);
}
