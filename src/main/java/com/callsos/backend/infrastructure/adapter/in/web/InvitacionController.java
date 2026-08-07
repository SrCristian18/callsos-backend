/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.in.GenerarInvitacionAgentePort;
import com.callsos.backend.infrastructure.adapter.in.web.dto.GenerarInvitacionRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.InvitacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada: COMANDO genera tokens de invitación para el
 * registro de agentes.
 *
 * POST /api/v1/invitaciones — solo COMANDO (ver SecurityConfig).
 * Body: { "unidadPolicialId": "..." }
 * Response 200: { "token": "...", "unidadPolicialId": "...", "fechaExpiracion": "..." }
 *
 * El token se comparte con el agente por un canal fuera de la app
 * (verbal, mensaje interno) — el sistema no lo distribuye automáticamente.
 */
@RestController
@RequestMapping("/api/v1/invitaciones")
public class InvitacionController {

    private final GenerarInvitacionAgentePort generarInvitacionPort;

    public InvitacionController(GenerarInvitacionAgentePort generarInvitacionPort) {
        this.generarInvitacionPort = generarInvitacionPort;
    }

    @PostMapping
    public ResponseEntity<InvitacionResponse> generar(
            @Valid @RequestBody GenerarInvitacionRequest request,
            Authentication authentication) {

        // El actorId de COMANDO sale del JWT (Authentication), no del body —
        // mismo criterio que StompAuthChannelInterceptor: no confiar en
        // que el cliente declare su propia identidad.
        String comandoActorId = authentication.getName();

        InvitacionAgente invitacion = generarInvitacionPort.ejecutar(
            request.getUnidadPolicialId(), comandoActorId);

        return ResponseEntity.ok(InvitacionResponse.desde(invitacion));
    }
}
