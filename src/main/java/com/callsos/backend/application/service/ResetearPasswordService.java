/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.TokenReseteoPassword;
import com.callsos.backend.domain.port.in.ResetearPasswordPort;
import com.callsos.backend.domain.port.out.TokenReseteoPasswordRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Épica 8 (hallazgo #6, Parte 2): implementación de ResetearPasswordPort.
 *
 * Orden de validación (mismo criterio que RegistrarDenuncianteService/
 * RegistrarAgenteConInvitacionService: validar la regla de negocio ANTES
 * de tocar cualquier repositorio):
 *   1. Las contraseñas coinciden.
 *   2. El token existe y está vigente (no usado, no expirado).
 * Recién ahí se hashea la nueva contraseña, se actualiza `usuarios` y se
 * marca el token como usado.
 */
public class ResetearPasswordService implements ResetearPasswordPort {

    private final TokenReseteoPasswordRepositoryPort tokenRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetearPasswordService(
            TokenReseteoPasswordRepositoryPort tokenRepository,
            UsuarioRepositoryPort usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.tokenRepository  = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder  = passwordEncoder;
    }

    @Override
    public void ejecutar(String tokenStr, String nuevaPassword, String confirmarPassword) {
        if (nuevaPassword == null || !nuevaPassword.equals(confirmarPassword)) {
            throw new IllegalStateException("Las contraseñas no coinciden.");
        }

        TokenReseteoPassword token = tokenRepository.buscarPorToken(tokenStr)
            .orElseThrow(() -> new IllegalStateException(
                "El token de reseteo no existe o ya no es válido."));

        if (!token.estaVigente()) {
            throw new IllegalStateException(
                "El token de reseteo ya no está vigente (usado o expirado).");
        }

        String passwordHash = passwordEncoder.encode(nuevaPassword);
        usuarioRepository.actualizarPassword(token.getActorId(), passwordHash);

        token.marcarUsado();
        tokenRepository.actualizar(token);
    }
}