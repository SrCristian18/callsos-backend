package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.in.ConsultarAgentesDisponiblesPorCaiPort;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AgenteDisponibleResponse;
import com.callsos.backend.infrastructure.adapter.in.web.mapper.AgenteMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST — recursos de CAI (unidad policial).
 *
 * FIX: resuelve el Gap 3 de deuda_backend.md — hasta ahora no existía
 * ningún endpoint para que el operador de un CAI viera qué agentes
 * disponibles tiene antes de asignar un incidente.
 *
 * Endpoints:
 *   GET /{caiId}/agentes/disponibles → OPERADOR_CAI / COMANDO
 */
@RestController
@RequestMapping("/api/v1/cais")
public class CaiController {

    private final ConsultarAgentesDisponiblesPorCaiPort consultarDisponibles;

    public CaiController(ConsultarAgentesDisponiblesPorCaiPort consultarDisponibles) {
        this.consultarDisponibles = consultarDisponibles;
    }

    /**
     * GET /{caiId}/agentes/disponibles — agentes en estado DISPONIBLE
     * dentro del CAI indicado.
     *
     * Nota: no valida que caiId coincida con el actorId del operador
     * autenticado (igual que /por-cai en IncidenteController, que confía
     * en el JWT para el CAI propio). Si en el futuro un operador debe
     * ver SOLO su propio CAI, agregar esa validación aquí.
     */
    @GetMapping("/{caiId}/agentes/disponibles")
    public ResponseEntity<List<AgenteDisponibleResponse>> agentesDisponibles(
            @PathVariable String caiId) {
        List<Agente> agentes = consultarDisponibles.ejecutar(caiId);
        return ResponseEntity.ok(AgenteMapper.toResponseList(agentes));
    }
}
