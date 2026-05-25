/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.ReporteAdministrativo;

/**
 * Puerto de entrada: el Comando genera un reporte administrativo del incidente.
 * Paso 3 y 11 del flujo funcional.
 */
public interface CrearReporteAdministrativoPort {
    
    /**
     * @param incidenteId  ID del incidente
     * @param autoridadId  ID del CAI o Comando que genera el reporte
     * @param resumen      Resumen administrativo
     * @return             Reporte creado y persistido
     */
    ReporteAdministrativo ejecutar(String incidenteId, String autoridadId, String resumen);
}
