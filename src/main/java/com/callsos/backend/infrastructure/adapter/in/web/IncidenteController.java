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
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CambiarEstadoRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.CrearIncidenteRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.IncidenteResponse;
import com.callsos.backend.infrastructure.adapter.in.web.mapper.IncidenteMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para el agregado Incidente.
 *
 * Responsabilidades:
 *   1. Recibir la petición HTTP y validar el DTO con @Valid.
 *   2. Traducir DTO → dominio mediante IncidenteMapper.
 *   3. Invocar el puerto de entrada correspondiente.
 *   4. Traducir dominio → DTO de respuesta y devolver HTTP status correcto.
 *
 * Lo que NO hace este adaptador:
 *   - No contiene lógica de negocio (eso es del dominio).
 *   - No conoce las implementaciones concretas (Services).
 *   - No accede a repositorios ni infraestructura.
 */

@RestController
@RequestMapping("/api/incidentes")
public class IncidenteController {
    
    private final CrearIncidentePort crearIncidente;
    private final CambiarEstadoIncidentePort cambiarEstado;
    private final ConsultarEstadoIncidentePort consultarEstado;
    private final AsignarCAIAIncidentePort asignarCAI;
    private final AsignarAgentePort asignarAgente;
    private final AtenderIncidentePort atenderIncidente;
    private final EvaluarIncidentePort evaluarIncidente;
    
    public IncidenteController(CrearIncidentePort crearIncidente,
                               CambiarEstadoIncidentePort cambiarEstado,
                               ConsultarEstadoIncidentePort consultarEstado,
                               AsignarCAIAIncidentePort asignarCAI,
                               AsignarAgentePort asignarAgente,
                               AtenderIncidentePort atenderIncidente,
                               EvaluarIncidentePort evaluarIncidente) {
        this.crearIncidente   = crearIncidente;
        this.cambiarEstado    = cambiarEstado;
        this.consultarEstado  = consultarEstado;
        this.asignarCAI       = asignarCAI;
        this.asignarAgente    = asignarAgente;
        this.atenderIncidente = atenderIncidente;
        this.evaluarIncidente = evaluarIncidente;
    
    }
    
    /** POST /api/incidentes — DENUNCIANTE crea el incidente. */
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
 
    /** GET /api/incidentes/{id}/estado */
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoIncidente> consultarEstado(@PathVariable String id) {
        return ResponseEntity.ok(consultarEstado.ejecutar(id));
    }
 
    /** PATCH /api/incidentes/{id}/estado — cambio manual (admin) */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        cambiarEstado.ejecutar(id, request.getNuevoEstado());
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /api/incidentes/{id}/derivar — COMANDO deriva al CAI más cercano. */
    @PatchMapping("/{id}/derivar")
    public ResponseEntity<Void> derivarACAI(@PathVariable String id) {
        asignarCAI.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /api/incidentes/{id}/asignar — OPERADOR_CAI asigna agente. */
    @PatchMapping("/{id}/asignar")
    public ResponseEntity<Void> asignarAgente(@PathVariable String id) {
        asignarAgente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /api/incidentes/{id}/atender — AGENTE llega al lugar. */
    @PatchMapping("/{id}/atender")
    public ResponseEntity<Void> atender(@PathVariable String id) {
        atenderIncidente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /api/incidentes/{id}/evaluar — AGENTE finaliza la atención. */
    @PatchMapping("/{id}/evaluar")
    public ResponseEntity<Void> evaluar(@PathVariable String id) {
        evaluarIncidente.ejecutar(id);
        return ResponseEntity.noContent().build();
    }
 
    /** PATCH /api/incidentes/{id}/cancelar — DENUNCIANTE cancela. */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        cambiarEstado.ejecutar(id, EstadoIncidente.CANCELADO);
        return ResponseEntity.noContent().build();
    }
}
