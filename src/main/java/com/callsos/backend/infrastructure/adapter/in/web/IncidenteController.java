package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.EtaInfo;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.*;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.ActualizarTipoIncidenteRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CambiarEstadoRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CrearIncidenteRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.EtaResponse;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.mapper.IncidenteMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final ConsultarIncidentesPorEstadoPort consultarPorEstado;
    private final AsignarCAIAIncidentePort asignarCAI;
    private final AsignarAgentePort asignarAgente;
    private final MarcarAgenteEnCaminoPort marcarEnCamino;
    private final AtenderIncidentePort atenderIncidente;
    private final EvaluarIncidentePort evaluarIncidente;
    private final SimularRecorridoAgentePort simularRecorrido;
    private final ActualizarTipoIncidentePort actualizarTipo;
    private final ConsultarEtaPort consultarEta;
    private final AsignacionRepositoryPort asignacionRepository;

    @Value("${SIMULACION_HABILITADA:false}")
    private boolean simulacionHabilitada;

    public IncidenteController(
            CrearIncidentePort crearIncidente,
            CambiarEstadoIncidentePort cambiarEstado,
            ConsultarEstadoIncidentePort consultarEstado,
            ConsultarIncidentePort consultarIncidente,
            ConsultarMisIncidentesPort consultarMisIncidentes,
            ConsultarIncidentesAsignadosPort consultarAsignados,
            ConsultarIncidentesPorCAIPort consultarPorCAI,
            ConsultarIncidentesPorEstadoPort consultarPorEstado,
            AsignarCAIAIncidentePort asignarCAI,
            AsignarAgentePort asignarAgente,
            MarcarAgenteEnCaminoPort marcarEnCamino,
            AtenderIncidentePort atenderIncidente,
            EvaluarIncidentePort evaluarIncidente,
            SimularRecorridoAgentePort simularRecorrido,
            ActualizarTipoIncidentePort actualizarTipo,
            ConsultarEtaPort consultarEta,
            AsignacionRepositoryPort asignacionRepository) {
        this.crearIncidente        = crearIncidente;
        this.cambiarEstado         = cambiarEstado;
        this.consultarEstado       = consultarEstado;
        this.consultarIncidente    = consultarIncidente;
        this.consultarMisIncidentes = consultarMisIncidentes;
        this.consultarAsignados    = consultarAsignados;
        this.consultarPorCAI       = consultarPorCAI;
        this.consultarPorEstado    = consultarPorEstado;
        this.asignarCAI            = asignarCAI;
        this.asignarAgente         = asignarAgente;
        this.marcarEnCamino        = marcarEnCamino;
        this.atenderIncidente      = atenderIncidente;
        this.evaluarIncidente      = evaluarIncidente;
        this.simularRecorrido      = simularRecorrido;
        this.actualizarTipo        = actualizarTipo;
        this.consultarEta          = consultarEta;
        this.asignacionRepository  = asignacionRepository;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /**
     * GET /{id} — detalle completo del incidente.
     *
     * Épica 7: incluye agenteId/nombreAgente (si hay asignación activa) —
     * es desde esta pantalla (DetalleIncidenteView) que Flutter navega a
     * TrackingView, y CAI/Comando necesitan saber a qué agente
     * suscribirse (/topic/agente/{agenteId}/ubicacion, Épica 3).
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidenteResponse> consultar(@PathVariable String id) {
        Incidente incidente = consultarIncidente.ejecutar(id);
        Asignacion asignacionActiva = asignacionRepository
            .buscarPorIncidente(id)
            .orElse(null);
        return ResponseEntity.ok(
            IncidenteMapper.toResponse(incidente, asignacionActiva));
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

    /**
     * GET /por-estado?estado=CREADO — listado de Comando.
     *
     * FIX (validación end-to-end): resuelve el Gap 2 de deuda_backend.md.
     * Devuelve todos los incidentes en el estado indicado sin filtrar por
     * actorId. Diseñado para COMANDO (ver HomeComandoView) — solo COMANDO
     * y OPERADOR_CAI tienen acceso a este endpoint (ver SecurityConfig).
     *
     * Ejemplo: GET /api/v1/incidentes/por-estado?estado=CREADO
     */
    @GetMapping("/por-estado")
    public ResponseEntity<List<IncidenteResponse>> porEstado(
            @RequestParam EstadoIncidente estado) {
        List<Incidente> incidentes = consultarPorEstado.ejecutar(estado);
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

    /**
     * PATCH /{id}/en-camino?simular=true
     *
     * El parámetro "simular" es SOLO PRUEBAS PILOTO: si se envía en true
     * Y ADEMÁS la propiedad simulacion.habilitada está en true en este
     * ambiente, el backend reemplaza el GPS real del agente por un
     * recorrido simulado (ver SimularRecorridoAgenteService).
     *
     * Si simulacion.habilitada=false (valor por defecto / producción),
     * el parámetro se ignora silenciosamente y el comportamiento es el
     * normal: se espera la posición real del celular del agente.
     */
    @PatchMapping("/{id}/en-camino")
    public ResponseEntity<Void> marcarEnCamino(
        @PathVariable String id,
        @RequestParam(required = false, defaultValue = "false") boolean simular) 
    {
        marcarEnCamino.ejecutar(id);
        if(simular && simulacionHabilitada)
        {
            simularRecorrido.iniciar(id);
        }
        return ResponseEntity.noContent().build();
    }
    
    /**
     * PATCH /{id}/detener-simulacion — SOLO PRUEBAS PILOTO.
     *
     * Permite al tester retomar el control manual del "agente" en
     * cualquier momento del trayecto simulado (ej: si necesita ajustar
     * algo antes de llegar). No tiene efecto si no hay una simulación
     * activa para ese incidente.
     */
    @PatchMapping("/{id}/detener-simulacion")
    public ResponseEntity<Void> detenerSimulacion(@PathVariable String id){
        simularRecorrido.detener(id);
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

    /**
     * PATCH /{id}/tipo — el DENUNCIANTE dueño actualiza el tipo de su
     * incidente mientras está activo (Épica 1).
     *
     * El actorId se extrae del JWT (Authentication), nunca del body —
     * mismo patrón de ownership que DenuncianteController.registrarToken.
     * La comparación real contra el denunciante dueño del incidente ocurre
     * dentro de ActualizarTipoIncidenteService (403 si no coincide).
     */
    @PatchMapping("/{id}/tipo")
    public ResponseEntity<Void> actualizarTipo(
            @PathVariable String id,
            @Valid @RequestBody ActualizarTipoIncidenteRequest request,
            Authentication authentication) {
        String actorId = authentication.getName();
        actualizarTipo.ejecutar(id, actorId, request.getNuevoTipo());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /{id}/eta — tiempo estimado de llegada del agente para el
     * denunciante dueño del incidente (Épica 4).
     *
     * Complementa el broadcast automático por WebSocket
     * (/topic/incidente/{id}/eta, ver PublicarUbicacionAgenteService):
     * este endpoint cubre el caso de reconexión — la app pide el valor
     * vigente sin esperar la próxima actualización GPS del agente.
     *
     * Nunca expone lat/lon — ver EtaInfo. Devuelve minutosEstimados y
     * categoriaDistancia en null (200, no error) cuando el incidente
     * aún no está en AGENTE_EN_CAMINO o el agente no ha reportado
     * posición todavía.
     */
    @GetMapping("/{id}/eta")
    public ResponseEntity<EtaResponse> eta(
            @PathVariable String id, Authentication authentication) {
        String actorId = authentication.getName();
        EtaInfo eta = consultarEta.consultar(id, actorId);
        return ResponseEntity.ok(
            new EtaResponse(eta.getMinutosEstimados(), eta.getCategoriaDistancia()));
    }
}