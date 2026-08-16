package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.ActualizarTipoIncidentePort;
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
 * Épica 1 — no publica todavía eventos de auditoría ni de propagación en
 * tiempo real (eso corresponde a Épicas 2 y 5, según lo acordado).
 */
public class ActualizarTipoIncidenteService implements ActualizarTipoIncidentePort {

    private final IncidenteRepositoryPort incidenteRepository;

    public ActualizarTipoIncidenteService(IncidenteRepositoryPort incidenteRepository) {
        this.incidenteRepository = incidenteRepository;
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

        incidente.cambiarTipo(nuevoTipo);

        incidenteRepository.guardar(incidente);
    }
}
