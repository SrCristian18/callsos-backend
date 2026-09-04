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
import com.callsos.backend.domain.port.in.RegistrarAgenteConInvitacionPort;
import com.callsos.backend.domain.port.in.RegistrarDenunciantePort;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AuthRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AuthResponse;
import com.callsos.backend.infrastructure.adapter.in.web.dto.RegistroAgenteRequest;
import com.callsos.backend.infrastructure.adapter.in.web.dto.RegistroDenuncianteRequest;
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
 * POST /api/v1/auth/registro/denunciante — público, sin autorización previa.
 * POST /api/v1/auth/registro/agente — público, pero requiere un token de
 *   invitación vigente generado antes por COMANDO (ver InvitacionController).
 *
 * Ambos registros devuelven el mismo shape que /login (autologueo).
 *
 * El token JWT se usa en Authorization: Bearer <token> en todos los demás endpoints.
 * actorId es el ID que Flutter necesita para identificarse (ej: registrar tokenFcm).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
 
    private final LoginPort loginPort;
    private final RegistrarDenunciantePort registrarDenunciantePort;
    private final RegistrarAgenteConInvitacionPort registrarAgentePort;
 
    public AuthController(LoginPort loginPort,
                           RegistrarDenunciantePort registrarDenunciantePort,
                           RegistrarAgenteConInvitacionPort registrarAgentePort) {
        this.loginPort                = loginPort;
        this.registrarDenunciantePort = registrarDenunciantePort;
        this.registrarAgentePort      = registrarAgentePort;
    }
 
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request) {
 
        LoginPort.LoginResultado resultado =
            loginPort.ejecutar(request.getUsername(), request.getPassword());
 
        return ResponseEntity.ok(new AuthResponse(
            resultado.token(),
            resultado.actorId(),
            resultado.rol(),
            resultado.nombre()
        ));
    }

    @PostMapping("/registro/denunciante")
    public ResponseEntity<AuthResponse> registrarDenunciante(
            @Valid @RequestBody RegistroDenuncianteRequest request) {

        LoginPort.LoginResultado resultado = registrarDenunciantePort.ejecutar(
            new RegistrarDenunciantePort.RegistroDenuncianteData(
                request.getNombre(),
                request.getApellido(),
                request.getDocumento(),
                request.getTelefono(),
                request.getCorreo(),
                request.getPassword(),
                request.getConfirmarPassword()
            ));

        return ResponseEntity.ok(new AuthResponse(
            resultado.token(), resultado.actorId(), resultado.rol(), resultado.nombre()));
    }

    @PostMapping("/registro/agente")
    public ResponseEntity<AuthResponse> registrarAgente(
            @Valid @RequestBody RegistroAgenteRequest request) {

        LoginPort.LoginResultado resultado = registrarAgentePort.ejecutar(
            new RegistrarAgenteConInvitacionPort.RegistroAgenteData(
                request.getToken(),
                request.getNombre(),
                request.getTelefono(),
                request.getCorreo(),
                request.getUsername(),
                request.getPassword(),
                request.getConfirmarPassword()
            ));

        return ResponseEntity.ok(new AuthResponse(
            resultado.token(), resultado.actorId(), resultado.rol(), resultado.nombre()));
    }
}