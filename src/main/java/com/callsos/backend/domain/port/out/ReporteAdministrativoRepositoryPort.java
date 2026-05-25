/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.ReporteAdministrativo;
 
import java.util.List;

public interface ReporteAdministrativoRepositoryPort {
    
    void guardar(ReporteAdministrativo reporte);
 
    List<ReporteAdministrativo> buscarPorIncidente(String incidenteId);
}
