/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UbicacionAgente;
import java.util.List;
 
/**
 * Puerto de salida: persistencia del historial de posiciones GPS del agente.
 */
public interface UbicacionAgenteRepositoryPort {
    
    /** Guarda la posición actual — llamado en cada actualización WebSocket. */
    void guardar(UbicacionAgente ubicacion);
 
    /**
     * Recupera el historial de posiciones de un agente en un incidente.
     * Útil para reconstruir la ruta del agente una vez finalizado el incidente.
     */
    List<UbicacionAgente> buscarPorIncidente(String incidenteId);
 
    /** Última posición conocida del agente — para mostrar al denunciante al reconectar. */
    java.util.Optional<UbicacionAgente> ultimaPosicion(String agenteId, String incidenteId);
}

