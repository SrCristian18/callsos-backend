/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AuthRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
/**
 * Adaptador de entrada: autenticación real con username + password.
 *
 * POST /api/v1/auth/login
 * Body: { "username": "juan.denunciante", "password": "password123" }
 * Response 200: { "token": "eyJ...", "actorId": "...", "rol": "DENUNCIANTE" }
 * Response 404: credenciales inválidas (IllegalArgumentException → GlobalExceptionHandler)
 *
 * El token JWT se usa en Authorization: Bearer <token> en todos los demás endpoints.
 * actorId es el ID que Flutter necesita para identificarse (ej: registrar tokenFcm).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
 
    private final LoginPort loginPort;
 
    public AuthController(LoginPort loginPort) {
        this.loginPort = loginPort;
    }
 
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request) {
 
        LoginPort.LoginResultado resultado =
            loginPort.ejecutar(request.getUsername(), request.getPassword());
 
        return ResponseEntity.ok(new AuthResponse(
            resultado.token(),
            resultado.actorId(),
            resultado.rol()
        ));
    }
}