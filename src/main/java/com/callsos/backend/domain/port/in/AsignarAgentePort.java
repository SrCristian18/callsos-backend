/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Incidente;
 
/**
 * Puerto de entrada: contrato para asignar un agente disponible a un incidente.
 * El servicio buscará internamente un agente libre de la unidad correspondiente.
 */
public interface AsignarAgentePort {
    
    /**
     * @param incidente  Incidente al que se asignará un agente
     */
    void ejecutar(Incidente incidente);
}
