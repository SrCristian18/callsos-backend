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
 * Puerto de salida: buscar un agente por su ID.
 * Separado de AgenteRepositoryPort para respetar el principio
 * de segregación de interfaces (ISP).
 */
public interface AgenteByIdRepositoryPort {
    
    Optional<Agente> buscarPorId(String id);
}
