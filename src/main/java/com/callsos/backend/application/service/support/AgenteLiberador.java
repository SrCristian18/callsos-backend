/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service.support;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;

import java.util.Optional;

/**
 * Colaborador compartido: libera al agente asignado a un incidente que
 * se está cerrando (FINALIZADO o CANCELADO).
 *
 * BUG DE PRODUCCIÓN QUE ESTE COLABORADOR CORRIGE: el agregado
 * {@link Asignacion} ya tenía un método {@code finalizar()} bien
 * diseñado — marca la asignación FINALIZADA y llama
 * {@code agente.liberar()} — desde que se implementó Asignacion.java.
 * El problema es que NINGÚN caso de uso lo invocaba nunca.
 * {@code EvaluarIncidenteService}, {@code CrearReporteHallazgosService}
 * (el flujo REALMENTE usado por la app — ver
 * {@code ReporteHallazgosView}, que llama directo a
 * {@code POST /reportes/hallazgos} y NUNCA a {@code PATCH /{id}/evaluar})
 * y {@code CambiarEstadoIncidenteService} (usado por {@code /cancelar})
 * solo transicionaban el ESTADO DEL INCIDENTE — nunca tocaban el Agente
 * ni la Asignacion. Resultado observado en producción: el agente
 * quedaba OCUPADO en BD para siempre tras cada incidente, sin ninguna
 * forma de que el sistema lo liberara solo — requería una corrección
 * manual directa en la tabla `agentes` cada vez.
 *
 * Se extrae como colaborador compartido, inyectado en los 3 casos de
 * uso que necesitan exactamente este efecto colateral al cerrar un
 * incidente, en vez de triplicar la misma secuencia de 3 pasos (buscar
 * asignación activa → finalizar() en memoria → persistir ambos lados)
 * copiada y pegada tres veces.
 */
public class AgenteLiberador {

    private final AsignacionRepositoryPort asignacionRepository;
    private final AgenteRepositoryPort agenteRepository;

    public AgenteLiberador(AsignacionRepositoryPort asignacionRepository,
                           AgenteRepositoryPort agenteRepository) {
        this.asignacionRepository = asignacionRepository;
        this.agenteRepository     = agenteRepository;
    }

    /**
     * Si el incidente tiene una Asignacion en estado ACTIVA, la finaliza
     * y libera al agente correspondiente (vuelve a DISPONIBLE),
     * persistiendo ambos lados: la fila de `asignaciones` (estado
     * ACTIVA → FINALIZADA) y la fila de `agentes` (estado
     * OCUPADO → DISPONIBLE).
     *
     * No-op silencioso si no hay asignación activa — caso legítimo, no
     * un error: un incidente cancelado en CREADO o DERIVADO_A_CAI, antes
     * de que se le asignara ningún agente, no tiene a quién liberar.
     */
    public void liberarSiHayAsignacionActiva(String incidenteId) {
        Optional<Asignacion> asignacionActiva =
            asignacionRepository.buscarPorIncidente(incidenteId);

        if (asignacionActiva.isEmpty()) return;

        Asignacion asignacion = asignacionActiva.get();
        asignacion.finalizar(); // estado -> FINALIZADA; agente.liberar() en memoria

        asignacionRepository.guardar(asignacion);
        agenteRepository.actualizarEstado(asignacion.getAgente());
    }
}