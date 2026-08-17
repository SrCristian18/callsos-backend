package com.callsos.backend.domain.event;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;

/**
 * Evento disparado cuando el denunciante actualiza el tipo de su incidente
 * (Épica 1: ActualizarTipoIncidenteService → Épica 2: auditoría integral).
 *
 * NO representa una transición de estado — el estado del incidente no
 * cambia al actualizar el tipo. Por eso estadoAnterior/estadoNuevo
 * (heredados de IncidenteEvent) llevan aquí el MISMO valor: el estado
 * vigente del incidente al momento del cambio. Son tipoAnterior/tipoNuevo
 * los que expresan el hecho real ocurrido.
 *
 * Extiende IncidenteEvent porque EventPublisherPort.publicar() solo acepta
 * IncidenteEvent (el dominio no expone un puerto de publicación separado
 * por tipo de evento) — no porque conceptualmente sea una transición.
 *
 * AuditoriaEventListener lo distingue explícitamente del resto de eventos
 * de IncidenteEvent para registrarlo como cambio de campo genérico
 * (AuditoriaIncidente.deCambioGenerico), no como transición de estado.
 */
public class TipoIncidenteActualizadoEvent extends IncidenteEvent {

    private final TipoIncidente tipoAnterior;
    private final TipoIncidente tipoNuevo;

    public TipoIncidenteActualizadoEvent(String incidenteId, String denuncianteId,
                                         EstadoIncidente estadoVigente,
                                         TipoIncidente tipoAnterior, TipoIncidente tipoNuevo) {
        super(incidenteId, denuncianteId, estadoVigente, estadoVigente);
        this.tipoAnterior = tipoAnterior;
        this.tipoNuevo    = tipoNuevo;
    }

    public TipoIncidente getTipoAnterior() { return tipoAnterior; }
    public TipoIncidente getTipoNuevo()    { return tipoNuevo; }
}
