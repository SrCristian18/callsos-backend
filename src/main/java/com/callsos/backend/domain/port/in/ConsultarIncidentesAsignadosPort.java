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
 * Puerto de entrada: incidentes activos asignados al agente autenticado.
 * Flutter lo usa para la pantalla "Cola de trabajo" del agente.
 */
public interface ConsultarIncidentesAsignadosPort {
    List<Incidente> ejecutar(String agenteId);
}
