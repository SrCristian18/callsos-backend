/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

import java.util.concurrent.ScheduledFuture;

/**
 * Puerto de salida: registro en memoria de qué incidentes tienen
 * actualmente una simulación piloto de recorrido de agente en curso, y
 * control de su tarea programada asociada.
 *
 * FIX (auditoría AUD-arquitectura): antes de este puerto,
 * {@code SimularRecorridoAgenteService} (application/service) importaba y
 * dependía directamente de {@code SimulacionEstado} (clase concreta en
 * {@code infrastructure.adapter.out.ruta}) — mismo tipo de violación de
 * dependencia hexagonal que {@link TokenGeneratorPort} corrige para JWT.
 *
 * Nota: esta es una utilidad "SOLO PRUEBAS PILOTO" (ver
 * {@code SimularRecorridoAgenteService}/{@code IncidenteController}), no
 * un mecanismo de producción — el puerto se agrega igual por consistencia
 * arquitectónica, no porque el riesgo funcional sea alto.
 */
public interface SimulacionEstadoPort {

    /** ¿Hay una simulación en curso para este incidente ahora mismo? */
    boolean estaSimulando(String incidenteId);

    /**
     * Registra la tarea programada de una simulación recién iniciada,
     * para poder cancelarla más adelante con {@link #detener}.
     */
    void registrar(String incidenteId, ScheduledFuture<?> tarea);

    /**
     * Cancela la tarea programada de la simulación de este incidente, si
     * existe. No hace nada si no había ninguna simulación activa.
     */
    void detener(String incidenteId);
}