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
 * Puerto de entrada: historial de incidentes del denunciante autenticado.
 * Flutter lo usa para la pantalla "Mis denuncias".
 */
public interface ConsultarMisIncidentesPort {
    List<Incidente> ejecutar(String denuncianteId);
}
