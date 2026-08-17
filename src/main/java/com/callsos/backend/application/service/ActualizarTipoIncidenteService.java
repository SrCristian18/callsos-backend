package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.ActualizarTipoIncidentePort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;

/**
 * Caso de uso: el denunciante actualiza el tipo de su incidente.
 *
 * Responsabilidades de este servicio (que el agregado Incidente NO conoce):
 *   1. Cargar el incidente.
 *   2. Validar OWNERSHIP — el actorId debe coincidir con el denunciante
 *      dueño del incidente. Si no coincide, lanza AccesoDenegadoException
 *      (403), sin filtrar si el incidente existe o no a un tercero no
 *      autorizado más allá de lo que ya expone el propio 403.
 *   3. Delegar la regla de negocio (estado activo, tipo distinto) al
 *      agregado vía incidente.cambiarTipo() — el dominio es quien decide
 *      si la transición es válida.
 *   4. Persistir.
 *
 * Épica 2 — ahora sí publica TipoIncidenteActualizadoEvent, capturado por
 * AuditoriaEventListener para dejar trazabilidad (valor anterior/nuevo)
 * del cambio de tipo. La propagación en tiempo real vía WebSocket es
 * responsabilidad de Épica 5, que reutilizará el mismo evento.
 */
public class ActualizarTipoIncidenteService implements ActualizarTipoIncidentePort {

    private final IncidenteRepositoryPort incidenteRepository;
    private final EventPublisherPort eventPublisher;

    public ActualizarTipoIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                          EventPublisherPort eventPublisher) {
        this.incidenteRepository = incidenteRepository;
        this.eventPublisher      = eventPublisher;
    }

    @Override
    public void ejecutar(String incidenteId, String actorId, TipoIncidente nuevoTipo) {

        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));

        if (!incidente.getDenunciante().getId().equals(actorId)) {
            throw new AccesoDenegadoException(
                "El denunciante autenticado no es el dueño de este incidente.");
        }

        TipoIncidente tipoAnterior = incidente.getTipo();

        incidente.cambiarTipo(nuevoTipo);

        incidenteRepository.guardar(incidente);

        eventPublisher.publicar(new TipoIncidenteActualizadoEvent(
            incidenteId, actorId, incidente.getEstado(), tipoAnterior, nuevoTipo));
    }
}