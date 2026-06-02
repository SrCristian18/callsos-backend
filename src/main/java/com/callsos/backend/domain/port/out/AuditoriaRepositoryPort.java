/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.AuditoriaIncidente;
import java.util.List;
 
public interface AuditoriaRepositoryPort {
    
    void registrar(AuditoriaIncidente auditoria);
 
    List<AuditoriaIncidente> buscarPorIncidente(String incidenteId);
}
