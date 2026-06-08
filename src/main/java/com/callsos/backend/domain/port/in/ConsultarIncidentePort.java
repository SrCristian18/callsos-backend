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
 * Puerto de entrada: obtener el detalle completo de un incidente por ID.
 * Flutter lo necesita para mostrar la pantalla de detalle del incidente.
 */
public interface ConsultarIncidentePort {
    Incidente ejecutar(String incidenteId);
}
