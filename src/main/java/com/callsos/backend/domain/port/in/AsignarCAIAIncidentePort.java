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
 * Puerto de entrada: contrato para derivar un incidente al CAI más cercano.
 *
 * Este es el paso que ejecuta el Comando entre recibir la denuncia
 * y asignar un agente. Sin este paso, AsignarAgenteService no puede
 * funcionar porque el incidente no tendría UnidadPolicial asignada.
 *
 * Flujo:
 *   CrearIncidente → [AsignarCAIAIncidente] → AsignarAgente → AtenderIncidente
 */
public interface AsignarCAIAIncidentePort {
    
    /**
     * @param incidenteId  ID del incidente a derivar
     */
    void ejecutar(String incidenteId);
}
