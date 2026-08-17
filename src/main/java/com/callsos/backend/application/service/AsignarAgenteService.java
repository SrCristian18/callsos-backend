/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.in.AsignarAgentePort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: asignar un agente disponible a un incidente.
 *
 * FIX 1 (contains → SQL): el filtro de agentes ahora se hace en BD
 *   por unidad_policial_id, no en memoria por referencia de objeto.
 *
 * FIX 2 (AsignacionRepositoryPort): la Asignacion ahora se persiste
 *   en BD a través del puerto de salida correspondiente.
 *   Antes se creaba en memoria pero nunca se guardaba en la tabla asignaciones.
 *
 * FIX 3 (Épica 4 — condición de carrera): antes, este método hacía un
 *   SELECT (obtenerDisponiblesPorUnidad) y, varias líneas después, un
 *   UPDATE ciego (agenteRepository.actualizarEstado) sin ninguna
 *   condición ni lock entre medio. Dos operadores de CAI asignando al
 *   mismo tiempo podían leer el mismo agente "disponible" antes de que
 *   cualquiera de los dos UPDATE aplicara, resultando en el mismo agente
 *   asignado a dos incidentes distintos simultáneamente.
 *
 *   Ahora cada candidato se "reclama" con un UPDATE condicional atómico
 *   (agenteRepository.intentarReservar) antes de comprometerse a usarlo.
 *   Si la reserva falla (alguien más lo tomó primero), se prueba con el
 *   siguiente candidato de la lista en vez de fallar de inmediato — solo
 *   se lanza IllegalStateException si NINGÚN candidato pudo reservarse.
 */
public class AsignarAgenteService implements AsignarAgentePort {
 
    private final AgenteRepositoryPort agenteRepository;
    private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
    private final EventPublisherPort eventPublisher;
 
    public AsignarAgenteService(AgenteRepositoryPort agenteRepository,
                                IncidenteRepositoryPort incidenteRepository,
                                AsignacionRepositoryPort asignacionRepository,
                                EventPublisherPort eventPublisher) {
        this.agenteRepository      = agenteRepository;
        this.incidenteRepository   = incidenteRepository;
        this.asignacionRepository  = asignacionRepository;
        this.eventPublisher        = eventPublisher;
    }
 
    @Override
    public void ejecutar(String incidenteId) {
 
        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        UnidadPolicial unidad = incidente.getUnidadPolicial();
        if (unidad == null)
            throw new IllegalStateException(
                "El incidente no tiene CAI asignado. " +
                "Ejecute /derivar antes de /asignar.");

        // Se valida ANTES de reservar cualquier agente a propósito: si esto
        // se revisara después de intentarReservar() (como en una versión
        // anterior de este método), un incidente sin Denuncia dejaría al
        // agente marcado OCUPADO en BD de forma permanente, sin ninguna
        // Asignacion real creada para liberarlo después — un "agente
        // atascado" sin forma de recuperarse solo.
        Denuncia denuncia = incidente.getDenuncia();
        if (denuncia == null)
            throw new IllegalStateException(
                "El incidente no tiene una Denuncia vinculada.");

        List<Agente> disponibles = agenteRepository
            .obtenerDisponiblesPorUnidad(unidad.getId());

        if (disponibles.isEmpty())
            throw new IllegalStateException(
                "No hay agentes disponibles en la unidad: " + unidad.getNombre());

        // Recorre los candidatos intentando reservar cada uno de forma
        // atómica hasta que uno tenga éxito. Si otra asignación
        // concurrente ya se quedó con el primer candidato entre el
        // SELECT de arriba y este punto, ese intentarReservar() devuelve
        // false y se prueba con el siguiente — sin volver a golpear la BD
        // con un nuevo SELECT.
        Agente agenteReservado = null;
        for (Agente candidato : disponibles) {
            if (agenteRepository.intentarReservar(candidato.getId())) {
                agenteReservado = candidato;
                break;
            }
        }

        if (agenteReservado == null)
            throw new IllegalStateException(
                "No hay agentes disponibles en la unidad: " + unidad.getNombre() +
                " (todos los candidatos fueron tomados por otra asignación concurrente).");
 
        // new Asignacion(...) llama agenteReservado.asignar() en memoria:
        // agenteReservado.estaDisponible() todavía es true en el objeto
        // Java (viene del SELECT de arriba, antes de la reserva), así que
        // el invariante del constructor pasa sin problema, y el estado en
        // memoria queda consistente con lo que ya escribimos en BD vía
        // intentarReservar().
        Asignacion asignacion = new Asignacion(
            UUID.randomUUID().toString(),
            agenteReservado,
            denuncia
        );
 
        EstadoIncidente estadoAnterior = incidente.getEstado();

        incidente.agregarAsignacion(asignacion);
        incidente.marcarAgenteAsignado();
 
        // Persistir asignación e incidente. El estado del agente YA se
        // persistió atómicamente en intentarReservar() — no hace falta
        // (ni conviene) un actualizarEstado() adicional acá.
        asignacionRepository.guardar(asignacion);
        incidenteRepository.guardar(incidente);

        // Épica 2 (fix P4): antes esta transición no quedaba auditada.
        eventPublisher.publicar(new IncidenteEvent(
            incidenteId, incidente.getDenunciante().getId(),
            estadoAnterior, EstadoIncidente.AGENTE_ASIGNADO));
    }
}