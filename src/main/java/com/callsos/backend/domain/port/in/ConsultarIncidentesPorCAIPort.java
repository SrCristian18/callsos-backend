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
import java.util.List;
 
/**
 * Puerto de entrada: incidentes activos de una unidad policial.
 * Flutter lo usa para el panel de operaciones del OPERADOR_CAI.
 */
public interface ConsultarIncidentesPorCAIPort {
    List<Incidente> ejecutar(String unidadPolicialId);
}
