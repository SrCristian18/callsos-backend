/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Agente;
 
import java.util.Optional;
 
/**
 * Puerto de salida: contrato de persistencia para Agente.
 * AsignarAgenteService lo usa para encontrar un agente disponible.
 */
public interface AgenteRepositoryPort {
    
    Optional<Agente> buscarAgenteDisponible(String unidadPolicialId);
 
    Agente guardar(Agente agente);
}
