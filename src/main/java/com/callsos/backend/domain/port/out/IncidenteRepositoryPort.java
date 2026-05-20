/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Incidente;
 
import java.util.Optional;
 
/**
 * Puerto de salida: contrato de persistencia para Incidente.
 *
 * El dominio define QUÉ necesita (guardar, buscar).
 * La infraestructura (BdMySql) define CÓMO se hace.
 * El dominio nunca importa JPA ni SQL — solo esta interfaz.
 */
public interface IncidenteRepositoryPort {
    
    void guardar(Incidente incidente);
 
    Optional<Incidente> buscarPorId(String id);
 
    void actualizarEstado(String id, EstadoIncidente estado);
}
