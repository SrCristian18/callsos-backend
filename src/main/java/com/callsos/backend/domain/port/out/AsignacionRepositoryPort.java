/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Asignacion;
 
import java.util.Optional;

/**
 * Puerto de salida: contrato de persistencia para Asignacion.
 */
public interface AsignacionRepositoryPort {
    
    void guardar(Asignacion asignacion);
 
    Optional<Asignacion> buscarPorIncidente(String dIncidenteId);
}
