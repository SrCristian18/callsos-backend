/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.AsignarAgentePort;
import com.callsos.backend.domain.port.in.AsignarCAIAIncidentePort;
import com.callsos.backend.domain.port.in.AtenderIncidentePort;
import com.callsos.backend.domain.port.in.CambiarEstadoIncidentePort;
import com.callsos.backend.domain.port.in.ConsultarEstadoIncidentePort;
import com.callsos.backend.domain.port.in.CrearIncidentePort;
import com.callsos.backend.domain.port.in.EvaluarIncidentePort;
import com.callsos.backend.domain.port.in.MarcarAgenteEnCaminoPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CambiarEstadoRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CrearIncidenteRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.mapper.IncidenteMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Adaptador de entrada REST para el agregado Incidente.
 *
 * Flujo completo de endpoints en orden de ejecución:
 *   POST   /api/incidentes            → DENUNCIANTE crea el incidente
 *   PATCH  /{id}/derivar              → COMANDO asigna el CAI más cercano
 *   PATCH  /{id}/asignar              → OPERADOR_CAI asigna un agente disponible
 *   PATCH  /{id}/en-camino            → AGENTE confirma que va en camino  ← NUEVO
 *   PATCH  /{id}/atender              → AGENTE llega al lugar (EN_ATENCION)
 *   PATCH  /{id}/evaluar              → AGENTE finaliza la atención
 *   PATCH  /{id}/cancelar             → DENUNCIANTE cancela el incidente
 *   GET    /{id}/estado               → cualquier actor consulta el estado
 */

@RestController
@RequestMapping("/api/v1/incidentes")
public class IncidenteController {
    
    private final CrearIncidentePort crearIncidente;
    private final CambiarEstadoIncidentePort cambiarEstado;
    private final ConsultarEstadoIncidentePort consultarEstado;
    private final AsignarCAIAIncidentePort asignarCAI;
    private final AsignarAgentePort asignarAgente;
    private final MarcarAgenteEnCaminoPort marcarEnCamino;
    private final AtenderIncidentePort atenderIncidente;
    private final EvaluarIncidentePort evaluarIncidente;
    
    public IncidenteController(CrearIncidentePort crearIncidente,
                               CambiarEstadoIncidentePort cambiarEstado,
                               ConsultarEstadoIncidentePort consultarEstado,
                               AsignarCAIAIncidentePort asignarCAI,
                               AsignarAgentePort asignarAgente,
                               MarcarAgenteEnCaminoPort marcarEnCamino,
                               AtenderIncidentePort atenderIncidente,
                               EvaluarIncidentePort evaluarIncidente) {
        this.crearIncidente   = crearIncidente;
        this.cambiarEstado    = cambiarEstado;
        this.consultarEstado  = consultarEstado;
        this.asignarCAI       = asignarCAI;
        this.asignarAgente    = asignarAgente;
        this.marcarEnCamino   = marcarEnCamino;
        this.atenderIncidente = atenderIncidente;
        this.evaluarIncidente = evaluarIncidente;
    
    }
    
    /** POST /api/incidentes — DENUNCIANTE crea el incidente. Responde 201. */
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
 
    /** GET /{id}/estado — consulta el estado actual. Solo lectura. */
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoIncidente> consultarEstado(@PathVariable String id) {
        return ResponseEntity.ok(consultarEstado.ejecutar(id));
    }
 
    /** PATCH /{id}/estado — cambio manual de estado (admin). */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        cambiarEstado.ejecutar(id, request.getNuevoEstado());
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /{id}/derivar — COMANDO deriva al CAI más cercano (Haversine). */
    @PatchMapping("/{id}/derivar")
    public ResponseEntity<Void> derivarACAI(@PathVariable String id) {
        asignarCAI.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /{id}/asignar — OPERADOR_CAI asigna agente disponible. */
    @PatchMapping("/{id}/asignar")
    public ResponseEntity<Void> asignarAgente(@PathVariable String id) {
        asignarAgente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /{id}/en-camino — AGENTE confirma que va en camino al lugar. */
    @PatchMapping("/{id}/en-camino")
    public ResponseEntity<Void> marcarEnCamino(@PathVariable String id) {
        marcarEnCamino.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /{id}/atender — AGENTE llega al lugar (AGENTE_EN_CAMINO → EN_ATENCION). */
    @PatchMapping("/{id}/atender")
    public ResponseEntity<Void> atender(@PathVariable String id) {
        atenderIncidente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /{id}/evaluar — AGENTE finaliza la atención (EN_ATENCION → FINALIZADO). */
    @PatchMapping("/{id}/evaluar")
    public ResponseEntity<Void> evaluar(@PathVariable String id) {
        evaluarIncidente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /{id}/cancelar — DENUNCIANTE cancela si ya no necesita intervención. */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id) {
        cambiarEstado.ejecutar(id, EstadoIncidente.CANCELADO);
        return ResponseEntity.noContent().build();
    }
}