/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.in.RegistrarTokenFcmPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
/**
 * Adaptador de entrada REST para el Denunciante.
 *
 * Flujo de registro del token FCM desde Flutter:
 *   1. Flutter hace login → recibe actorId en AuthResponse
 *   2. Firebase SDK emite un token en el dispositivo
 *   3. Flutter llama a PATCH /api/v1/denunciantes/{actorId}/token
 *      con Authorization: Bearer <jwt>
 *   4. El backend guarda el token en BD
 *   5. A partir de ese momento, NotificacionFirebaseAdapter puede
 *      enviar notificaciones push al dispositivo del denunciante
 *
 * Por qué PATCH y no POST:
 *   El denunciante ya existe — solo se actualiza un campo.
 *   PATCH es semánticamente correcto para actualizaciones parciales.
 */
@RestController
@RequestMapping("/api/v1/denunciantes")
public class DenuncianteController {
    
    private final RegistrarTokenFcmPort registrarTokenFcm;
 
    public DenuncianteController(RegistrarTokenFcmPort registrarTokenFcm) {
        this.registrarTokenFcm = registrarTokenFcm;
    }
 
    /**
     * PATCH /api/v1/denunciantes/{id}/token
     *
     * Registra o actualiza el token FCM del denunciante.
     * Solo el DENUNCIANTE autenticado puede actualizar su propio token
     * (SecurityConfig protege este endpoint con hasRole("DENUNCIANTE")).
     *
     * Body: { "tokenFcm": "ePWiK3M7..." }
     * Response: 204 No Content
     */
    @PatchMapping("/{id}/token")
    public ResponseEntity<Void> registrarToken(
            @PathVariable String id,
            @Valid @RequestBody TokenFcmRequest request) {
 
        registrarTokenFcm.ejecutar(id, request.tokenFcm());
        return ResponseEntity.noContent().build();
    }
 
    // DTO inline — record Java 21
    public record TokenFcmRequest(
        @NotBlank(message = "El tokenFcm es obligatorio")
        String tokenFcm
    ) {}
}
