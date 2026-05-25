/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.ReporteAdministrativo;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.port.in.CrearReporteAdministrativoPort;
import com.callsos.backend.domain.port.in.CrearReporteHallazgosPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.time.LocalDateTime;
 
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    
    private final CrearReporteHallazgosPort crearHallazgos;
    private final CrearReporteAdministrativoPort crearAdministrativo;
 
    public ReporteController(CrearReporteHallazgosPort crearHallazgos,
                             CrearReporteAdministrativoPort crearAdministrativo) {
        this.crearHallazgos     = crearHallazgos;
        this.crearAdministrativo = crearAdministrativo;
    }
 
    /** POST /api/reportes/hallazgos — el agente reporta hallazgos al finalizar. */
    @PostMapping("/hallazgos")
    public ResponseEntity<ReporteHallazgosResponse> crearHallazgos(
            @Valid @RequestBody ReporteHallazgosRequest request) {
 
        ReporteHallazgos reporte = crearHallazgos.ejecutar(
            request.incidenteId(), request.agenteId(), request.descripcion());
 
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ReporteHallazgosResponse(
                reporte.getId(), reporte.getFecha(),
                reporte.getIncidente().getId(), reporte.getAgente().getId()));
    }
 
    /** POST /api/reportes/administrativo — el Comando genera reporte administrativo. */
    @PostMapping("/administrativo")
    public ResponseEntity<ReporteAdministrativoResponse> crearAdministrativo(
            @Valid @RequestBody ReporteAdministrativoRequest request) {
 
        ReporteAdministrativo reporte = crearAdministrativo.ejecutar(
            request.incidenteId(), request.autoridadId(), request.resumen());
 
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ReporteAdministrativoResponse(
                reporte.getId(), reporte.getFecha(),
                reporte.getIncidente().getId(), reporte.getAutoridad().getId()));
    }
 
    // ── DTOs inline (records Java 21) ─────────────────────────────────────
 
    public record ReporteHallazgosRequest(
        @NotBlank String incidenteId,
        @NotBlank String agenteId,
        @NotBlank String descripcion
    ) {}
 
    public record ReporteAdministrativoRequest(
        @NotBlank String incidenteId,
        @NotBlank String autoridadId,
        @NotBlank String resumen
    ) {}
 
    public record ReporteHallazgosResponse(
        String id, LocalDateTime fecha, String incidenteId, String agenteId
    ) {}
 
    public record ReporteAdministrativoResponse(
        String id, LocalDateTime fecha, String incidenteId, String autoridadId
    ) {}
}
