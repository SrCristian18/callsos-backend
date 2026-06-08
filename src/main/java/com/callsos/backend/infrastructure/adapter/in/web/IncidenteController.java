package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.*;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CambiarEstadoRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CrearIncidenteRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.mapper.IncidenteMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adaptador de entrada REST — Incidente.
 *
 * Endpoints de COMANDO (mutaciones de estado):
 *   POST   /                  → crear incidente
 *   PATCH  /{id}/derivar      → COMANDO: derivar al CAI más cercano
 *   PATCH  /{id}/asignar      → OPERADOR_CAI: asignar agente
 *   PATCH  /{id}/en-camino    → AGENTE: confirma que va en camino
 *   PATCH  /{id}/atender      → AGENTE: llegó al lugar
 *   PATCH  /{id}/evaluar      → AGENTE: finaliza la atención
 *   PATCH  /{id}/cancelar     → DENUNCIANTE: cancela
 *
 * Endpoints de CONSULTA (Fase E — lo que Flutter necesita para sus pantallas):
 *   GET    /{id}              → detalle completo del incidente
 *   GET    /{id}/estado       → estado actual (ya existía)
 *   GET    /mis-incidentes    → historial del denunciante autenticado
 *   GET    /asignados         → cola de trabajo del agente autenticado
 *   GET    /por-cai           → panel de operaciones del CAI
 */
@RestController
@RequestMapping("/api/v1/incidentes")
public class IncidenteController {

    private final CrearIncidentePort crearIncidente;
    private final CambiarEstadoIncidentePort cambiarEstado;
    private final ConsultarEstadoIncidentePort consultarEstado;
    private final ConsultarIncidentePort consultarIncidente;
    private final ConsultarMisIncidentesPort consultarMisIncidentes;
    private final ConsultarIncidentesAsignadosPort consultarAsignados;
    private final ConsultarIncidentesPorCAIPort consultarPorCAI;
    private final AsignarCAIAIncidentePort asignarCAI;
    private final AsignarAgentePort asignarAgente;
    private final MarcarAgenteEnCaminoPort marcarEnCamino;
    private final AtenderIncidentePort atenderIncidente;
    private final EvaluarIncidentePort evaluarIncidente;

    public IncidenteController(
            CrearIncidentePort crearIncidente,
            CambiarEstadoIncidentePort cambiarEstado,
            ConsultarEstadoIncidentePort consultarEstado,
            ConsultarIncidentePort consultarIncidente,
            ConsultarMisIncidentesPort consultarMisIncidentes,
            ConsultarIncidentesAsignadosPort consultarAsignados,
            ConsultarIncidentesPorCAIPort consultarPorCAI,
            AsignarCAIAIncidentePort asignarCAI,
            AsignarAgentePort asignarAgente,
            MarcarAgenteEnCaminoPort marcarEnCamino,
            AtenderIncidentePort atenderIncidente,
            EvaluarIncidentePort evaluarIncidente) {
        this.crearIncidente        = crearIncidente;
        this.cambiarEstado         = cambiarEstado;
        this.consultarEstado       = consultarEstado;
        this.consultarIncidente    = consultarIncidente;
        this.consultarMisIncidentes = consultarMisIncidentes;
        this.consultarAsignados    = consultarAsignados;
        this.consultarPorCAI       = consultarPorCAI;
        this.asignarCAI            = asignarCAI;
        this.asignarAgente         = asignarAgente;
        this.marcarEnCamino        = marcarEnCamino;
        this.atenderIncidente      = atenderIncidente;
        this.evaluarIncidente      = evaluarIncidente;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** GET /{id} — detalle completo del incidente. */
    @GetMapping("/{id}")
    public ResponseEntity<IncidenteResponse> consultar(@PathVariable String id) {
        return ResponseEntity.ok(
            IncidenteMapper.toResponse(consultarIncidente.ejecutar(id)));
    }

    /** GET /{id}/estado — estado actual. Solo lectura. */
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoIncidente> consultarEstado(@PathVariable String id) {
        return ResponseEntity.ok(consultarEstado.ejecutar(id));
    }

    /**
     * GET /mis-incidentes — historial del denunciante autenticado.
     * El actorId se extrae del JWT — el denunciante solo ve sus propios incidentes.
     */
    @GetMapping("/mis-incidentes")
    public ResponseEntity<List<IncidenteResponse>> misIncidentes(
            Authentication authentication) {
        String denuncianteId = authentication.getName(); // actorId del JWT
        List<Incidente> incidentes = consultarMisIncidentes.ejecutar(denuncianteId);
        return ResponseEntity.ok(IncidenteMapper.toResponseList(incidentes));
    }

    /**
     * GET /asignados — cola de trabajo del agente autenticado.
     * El actorId del JWT identifica al agente — solo ve sus incidentes activos.
     */
    @GetMapping("/asignados")
    public ResponseEntity<List<IncidenteResponse>> asignados(
            Authentication authentication) {
        String agenteId = authentication.getName();
        List<Incidente> incidentes = consultarAsignados.ejecutar(agenteId);
        return ResponseEntity.ok(IncidenteMapper.toResponseList(incidentes));
    }

    /**
     * GET /por-cai — panel de operaciones del CAI.
     * El actorId del JWT es el ID de la unidad policial del operador.
     */
    @GetMapping("/por-cai")
    public ResponseEntity<List<IncidenteResponse>> porCAI(
            Authentication authentication) {
        String unidadId = authentication.getName();
        List<Incidente> incidentes = consultarPorCAI.ejecutar(unidadId);
        return ResponseEntity.ok(IncidenteMapper.toResponseList(incidentes));
    }

    // ── Mutaciones de estado ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<IncidenteResponse> crear(
            @Valid @RequestBody CrearIncidenteRequest request) {
        Ubicacion ubicacion = IncidenteMapper.toUbicacion(request.getUbicacion());
        Incidente incidente = crearIncidente.ejecutar(
            request.getDenuncianteId(), request.getTipo(),
            request.getDescripcion(), ubicacion);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(IncidenteMapper.toResponse(incidente));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        cambiarEstado.ejecutar(id, request.getNuevoEstado());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/derivar")
    public ResponseEntity<Void> derivarACAI(@PathVariable String id) {
        asignarCAI.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<Void> asignarAgente(@PathVariable String id) {
        asignarAgente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/en-camino")
    public ResponseEntity<Void> marcarEnCamino(@PathVariable String id) {
        marcarEnCamino.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/atender")
    public ResponseEntity<Void> atender(@PathVariable String id) {
        atenderIncidente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/evaluar")
    public ResponseEntity<Void> evaluar(@PathVariable String id) {
        evaluarIncidente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id) {
        cambiarEstado.ejecutar(id, EstadoIncidente.CANCELADO);
        return ResponseEntity.noContent().build();
    }
}