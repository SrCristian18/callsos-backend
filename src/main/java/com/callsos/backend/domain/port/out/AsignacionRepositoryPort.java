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

    /**
     * Igual que {@link #buscarPorIncidente}, pero SIN filtrar por estado
     * ACTIVA — devuelve la asignación más reciente del incidente sin
     * importar si ya fue FINALIZADA (AUD-5).
     *
     * Necesario porque AgenteLiberador.liberarSiHayAsignacionActiva()
     * marca la asignación como FINALIZADA de forma SÍNCRONA, ANTES de
     * publicar el evento — para cuando el listener asíncrono de
     * notificaciones corre, buscarPorIncidente() (filtrado a ACTIVA) ya
     * no encuentra nada. Este método permite identificar igual "quién
     * era el agente asignado" para notificarle que su incidente fue
     * cancelado.
     */
    Optional<Asignacion> buscarUltimaPorIncidente(String incidenteId);
}