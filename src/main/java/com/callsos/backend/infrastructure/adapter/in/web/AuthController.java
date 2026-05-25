/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.infrastructure.adapter.in.web.dto.AuthRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AuthResponse;
import com.callsos.backend.infrastructure.config.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
/**
 * Adaptador de entrada: autenticación y emisión de JWT.
 *
 * FASE 1 — versión simplificada:
 *   Acepta cualquier userId + rol válido y emite un token.
 *   No valida contra BD de usuarios (eso es Fase 2 con tabla usuarios).
 *
 *   Uso para pruebas con Postman/Insomnia:
 *   POST /api/auth/token
 *   { "userId": "test-denunciante-001", "rol": "DENUNCIANTE" }
 *   → { "token": "eyJ...", "userId": "...", "rol": "DENUNCIANTE" }
 *
 *   El token se usa en los demás endpoints:
 *   Authorization: Bearer eyJ...
 *
 * FASE 2: validar userId contra tabla denunciantes/agentes según el rol.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
     private final JwtService jwtService;
 
    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }
 
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> generarToken(
            @Valid @RequestBody AuthRequest request) {
 
        // Validar que el rol sea uno de los permitidos
        try {
            com.callsos.backend.domain.enums.RolUsuario.valueOf(request.getRol());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Rol inválido: " + request.getRol() +
                ". Roles válidos: DENUNCIANTE, AGENTE, OPERADOR_CAI, COMANDO");
        }
 
        String token = jwtService.generarToken(request.getUserId(), request.getRol());
        return ResponseEntity.ok(
            new AuthResponse(token, request.getUserId(), request.getRol()));
    }
}
