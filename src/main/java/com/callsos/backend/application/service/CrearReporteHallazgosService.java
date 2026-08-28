/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.application.service.support.AgenteLiberador;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.event.IncidenteFinalizadoEvent;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.port.in.CrearReporteHallazgosPort;
import com.callsos.backend.domain.port.out.AgenteByIdRepositoryPort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.UUID;
 
/**
 * Caso de uso: el agente finaliza la atención y envía su reporte de hallazgos.
 *
 * Regla de negocio (clase ternaria):
 *   Sin Incidente + Agente → no existe ReporteHallazgos.
 *   Ambas referencias se validan antes de crear el reporte.
 *
 * Efecto colateral: el incidente transiciona a FINALIZADO.
 *
 * ESTE es el flujo que realmente usa la app para finalizar (ver
 * ReporteHallazgosView — llama directo a POST /reportes/hallazgos y
 * NUNCA a PATCH /{id}/evaluar). Por eso el fix del agente que queda
 * OCUPADO para siempre vive acá con la misma prioridad que en
 * EvaluarIncidenteService — ver el docstring de AgenteLiberador.
 *
 * FIX (Épica 8, auditoría de regresión): este servicio NUNCA publicaba
 * ningún evento de dominio — a diferencia de EvaluarIncidenteService (su
 * hermano, casi idéntico, que sí publica IncidenteFinalizadoEvent), a
 * este le faltaba el EventPublisherPort por completo. Como es EL flujo
 * real de finalización, el efecto era doble: (1) AuditoriaEventListener
 * nunca registraba la transición a FINALIZADO en el historial real de
 * ningún incidente — el criterio de aceptación de Épica 2
 * ("...finalización/cancelación...") quedaba incumplido en la práctica
 * para el 100% de las finalizaciones reales; (2) el denunciante nunca
 * recibía la notificación push "Tu incidente ha sido atendido
 * exitosamente" (NotificacionEventListener.onIncidenteFinalizado nunca
 * se disparaba, aunque el mensaje ya estaba escrito y listo).
 */
public class CrearReporteHallazgosService implements CrearReporteHallazgosPort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final AgenteByIdRepositoryPort agenteRepository;
    private final ReporteHallazgosRepositoryPort reporteRepository;
    private final AgenteLiberador agenteLiberador;
    private final EventPublisherPort eventPublisher;
 
    public CrearReporteHallazgosService(
            IncidenteRepositoryPort incidenteRepository,
            AgenteByIdRepositoryPort agenteRepository,
            ReporteHallazgosRepositoryPort reporteRepository,
            AgenteLiberador agenteLiberador,
            EventPublisherPort eventPublisher) {
        this.incidenteRepository = incidenteRepository;
        this.agenteRepository    = agenteRepository;
        this.reporteRepository   = reporteRepository;
        this.agenteLiberador     = agenteLiberador;
        this.eventPublisher      = eventPublisher;
    }
 
    @Override
    @Transactional
    // FIX (Épica 8): reporte.guardar() + incidente.guardar() + (vía
    // AgenteLiberador) asignacion.guardar() + agente.actualizarEstado()
    // son 4 escrituras en 4 tablas distintas — la peor expuesta de las
    // 5, sin protección transaccional un fallo a mitad de camino podía
    // dejar un reporte guardado con el incidente todavía EN_ATENCION, o
    // el incidente FINALIZADO con el agente todavía OCUPADO.
    public ReporteHallazgos ejecutar(String incidenteId, String agenteId, String descripcion) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        Agente agente = agenteRepository
            .buscarPorId(agenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Agente no encontrado: " + agenteId));
 
        // Validar que el incidente esté en estado atendible
        if (!incidente.getEstado().name().equals("EN_ATENCION"))
            throw new IllegalStateException(
                "Solo se puede reportar sobre un incidente EN_ATENCION. " +
                "Estado actual: " + incidente.getEstado());
 
        ReporteHallazgos reporte = new ReporteHallazgos(
            UUID.randomUUID().toString(),
            descripcion,
            incidente,
            agente
        );
 
        reporteRepository.guardar(reporte);
 
        // Finalizar el incidente tras el reporte
        EstadoIncidente estadoAnterior = incidente.getEstado();
        incidente.finalizar();
        incidenteRepository.guardar(incidente);
 
        // FIX: antes de este cambio, este era el punto exacto donde el
        // agente quedaba OCUPADO en BD para siempre — ver
        // AgenteLiberador para el detalle completo.
        agenteLiberador.liberarSiHayAsignacionActiva(incidenteId);

        // FIX (Épica 8): antes de este cambio, este servicio no publicaba
        // ningún evento — ver el docstring de la clase. Auditoría y
        // notificación push al denunciante dependen de este evento.
        eventPublisher.publicar(new IncidenteFinalizadoEvent(
            incidenteId,
            incidente.getDenunciante().getId(),
            estadoAnterior,
            incidente.getEstado()
        ));

        return reporte;
    }
}