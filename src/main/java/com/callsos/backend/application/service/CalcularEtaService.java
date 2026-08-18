package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.EtaInfo;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.in.ConsultarEtaPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.service.CalculadoraDistancia;

import java.util.Optional;

/**
 * Caso de uso: calcular el tiempo estimado de llegada del agente para el
 * denunciante (Épica 4), sin exponer coordenadas — apoyado en el modelo
 * de topics seguro de Épica 3 (P6 ya resuelto: el denunciante nunca
 * puede suscribirse al topic de ubicación cruda del agente).
 *
 * Reglas, en orden (mismo patrón defensivo que ActualizarTipoIncidenteService:
 * ownership ANTES que estado, para no filtrarle a un tercero no autorizado
 * en qué estado está un incidente que no le pertenece):
 *   1. Incidente debe existir → si no, 404 (IllegalArgumentException).
 *   2. El actor debe ser el denunciante dueño → si no, 403 (AccesoDenegadoException).
 *   3. Si el incidente no está en AGENTE_EN_CAMINO, o el agente todavía no
 *      reportó ninguna posición GPS, se retorna EtaInfo.sinDatos() en vez
 *      de lanzar una excepción — no es un error, es un estado transitorio
 *      normal del ciclo de vida (agente recién asignado, o ya finalizado).
 */
public class CalcularEtaService implements ConsultarEtaPort {

    private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
    private final UbicacionAgenteRepositoryPort ubicacionAgenteRepository;
    private final double velocidadMediaKmh;

    public CalcularEtaService(IncidenteRepositoryPort incidenteRepository,
                              AsignacionRepositoryPort asignacionRepository,
                              UbicacionAgenteRepositoryPort ubicacionAgenteRepository,
                              double velocidadMediaKmh) {
        this.incidenteRepository      = incidenteRepository;
        this.asignacionRepository     = asignacionRepository;
        this.ubicacionAgenteRepository = ubicacionAgenteRepository;
        this.velocidadMediaKmh        = velocidadMediaKmh;
    }

    @Override
    public EtaInfo consultar(String incidenteId, String actorId) {

        Incidente incidente = incidenteRepository
            .buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));

        if (!incidente.getDenunciante().getId().equals(actorId)) {
            throw new AccesoDenegadoException(
                "El denunciante autenticado no es el dueño de este incidente.");
        }

        if (incidente.getEstado() != EstadoIncidente.AGENTE_EN_CAMINO) {
            return EtaInfo.sinDatos();
        }

        Optional<String> agenteId = asignacionRepository
            .buscarPorIncidente(incidenteId)
            .map(a -> a.getAgente().getId());

        if (agenteId.isEmpty()) {
            return EtaInfo.sinDatos();
        }

        Optional<UbicacionAgente> ultimaPosicion =
            ubicacionAgenteRepository.ultimaPosicion(agenteId.get(), incidenteId);

        if (ultimaPosicion.isEmpty()) {
            // Agente asignado y en camino, pero aún no reportó ninguna
            // posición GPS (recién confirmó "en-camino" hace un instante).
            return EtaInfo.sinDatos();
        }

        double distanciaMetros = CalculadoraDistancia.distanciaMetros(
            ultimaPosicion.get().getUbicacion(), incidente.getUbicacion());

        return EtaInfo.calcular(distanciaMetros, velocidadMediaKmh);
    }
}