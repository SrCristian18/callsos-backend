/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */


import com.callsos.backend.domain.valueobject.Ubicacion;
import java.time.LocalDateTime;
 
/**
 * Registro de una posición GPS del agente en un momento dado.
 *
 * Inmutable — cada posición es un hecho histórico.
 * La tabla ubicaciones_agente ya existe en schema.sql (creada en Fase 0).
 *
 * Uso en tiempo real:
 *   1. App del agente envía lat/lon vía WebSocket
 *   2. UbicacionAgenteController crea esta entidad
 *   3. Se persiste en BD para historial
 *   4. Se publica a /topic/incidente/{id}/ubicacion para el denunciante
 */
public class UbicacionAgente {
    
    private final String agenteId;
    private final String incidenteId;
    private final Ubicacion ubicacion;
    private final LocalDateTime timestamp;
 
    public UbicacionAgente(String agenteId, String incidenteId,
                           Ubicacion ubicacion) {
        this.agenteId    = agenteId;
        this.incidenteId = incidenteId;
        this.ubicacion   = ubicacion;
        this.timestamp   = LocalDateTime.now();
    }
 
    public String getAgenteId()      { return agenteId; }
    public String getIncidenteId()   { return incidenteId; }
    public Ubicacion getUbicacion()  { return ubicacion; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
