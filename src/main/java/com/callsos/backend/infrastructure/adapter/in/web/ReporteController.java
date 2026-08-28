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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.time.LocalDateTime;
 
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {
    
    private final CrearReporteHallazgosPort crearHallazgos;
    private final CrearReporteAdministrativoPort crearAdministrativo;
 
    public ReporteController(CrearReporteHallazgosPort crearHallazgos,
                             CrearReporteAdministrativoPort crearAdministrativo) {
        this.crearHallazgos     = crearHallazgos;
        this.crearAdministrativo = crearAdministrativo;
    }
 
    /**
     * POST /api/reportes/hallazgos — el agente reporta hallazgos al finalizar.
     *
     * FIX (Épica 8, hallazgo de seguridad #1): antes el "agenteId" salía
     * DIRECTAMENTE del body de la petición (ReporteHallazgosRequest lo
     * declaraba como campo) — cualquier agente autenticado podía firmar
     * un reporte a nombre de OTRO agente con solo cambiar ese valor en
     * el JSON. Rompía la cadena de responsabilidad de un reporte
     * policial, y era la única excepción a la convención que sigue el
     * resto del sistema (AgenteController, CaiController,
     * DenuncianteController, ActualizarTipoIncidenteService, etc.):
     * el actorId SIEMPRE sale de authentication.getName() (el JWT), y
     * nunca de algo que el cliente declara sobre sí mismo.
     *
     * El campo "agenteId" se retiró del DTO de request — ya no tiene
     * ningún efecto que pudiera falsificarse, así que mantenerlo ahí
     * (aunque se ignorara) solo invitaría a confusión futura.
     */
    @PostMapping("/hallazgos")
    public ResponseEntity<ReporteHallazgosResponse> crearHallazgos(
            @Valid @RequestBody ReporteHallazgosRequest request,
            Authentication authentication) {
 
        String agenteIdDelJwt = authentication.getName();

        ReporteHallazgos reporte = crearHallazgos.ejecutar(
            request.incidenteId(), agenteIdDelJwt, request.descripcion());
 
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