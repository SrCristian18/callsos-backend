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
 *   2. UbicacionAgenteController delega en PublicarUbicacionAgentePort
 *   3. Se persiste en BD para historial
 *   4. Se publica a /topic/agente/{agenteId}/ubicacion — visible SOLO
 *      para el propio agente, su CAI, o COMANDO (Épica 3, fix P6). El
 *      denunciante NUNCA está entre los suscriptores autorizados de
 *      este topic — ver StompAuthChannelInterceptor.
 */
public class UbicacionAgente {
    
    private final String agenteId;
    private final String incidenteId;
    private final Ubicacion ubicacion;
    private LocalDateTime timestamp;
 
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

    // ── Reconstitución desde persistencia ────────────────────────────────

    /**
     * Restaura el timestamp real leído de BD (AUD-4).
     *
     * BUG encontrado: el RowMapper de UbicacionAgenteRepositoryMySQL
     * (buscarPorIncidente y ultimaPosicion) seleccionaba la columna
     * "timestamp" en el SQL pero reconstruía el objeto con
     * `new UbicacionAgente(agenteId, incidenteId, ubicacion)` — ese
     * constructor SIEMPRE fija timestamp = LocalDateTime.now(). El
     * resultado: cualquier posición leída de BD (no la que se acaba de
     * guardar) reportaba la hora de la CONSULTA, no la hora real en que
     * el agente estuvo ahí. Esto es lo que
     * UbicacionAgenteController.solicitarUltimaPosicion() envía al
     * cliente como "timestamp" al reconectar — el dato llegaba siempre
     * como "ahora", ocultando si la posición está desactualizada (agente
     * con la app cerrada, sin señal, etc.).
     *
     * SOLO para uso de adaptadores de persistencia al reconstituir el
     * agregado. Nunca llamar desde lógica de negocio — mismo patrón que
     * Incidente.reconstituirEstado()/reconstituirUnidad().
     */
    public void reconstituirTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}