/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.ReporteHallazgos;
/**
 * Puerto de entrada: el agente finaliza la atención y envía el reporte de hallazgos.
 * Paso 9 del flujo funcional.
 */
public interface CrearReporteHallazgosPort {
    
    /**
     * @param incidenteId  ID del incidente atendido
     * @param agenteId     ID del agente que reporta
     * @param descripcion  Descripción de los hallazgos en campo
     * @return             Reporte creado y persistido
     */
    ReporteHallazgos ejecutar(String incidenteId, String agenteId, String descripcion);
}
