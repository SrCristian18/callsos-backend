/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.TokenReseteoPassword;
import com.callsos.backend.domain.port.out.TokenReseteoPasswordRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Épica 8 (hallazgo #6, Parte 2).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResetearPasswordService")
class ResetearPasswordServiceTest {

    @Mock TokenReseteoPasswordRepositoryPort tokenRepository;
    @Mock UsuarioRepositoryPort usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;

    ResetearPasswordService service;

    @BeforeEach
    void setUp() {
        service = new ResetearPasswordService(tokenRepository, usuarioRepository, passwordEncoder);
    }

    private TokenReseteoPassword tokenVigente() {
        return TokenReseteoPassword.reconstituir(
            "token-abc123", "den-001",
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().plusMinutes(25),
            false, null);
    }

    @Test
    @DisplayName("token vigente y contraseñas coinciden: actualiza el hash y marca el token usado")
    void reseteoExitoso() {
        TokenReseteoPassword token = tokenVigente();
        when(tokenRepository.buscarPorToken("token-abc123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaPassword123")).thenReturn("hash-bcrypt-nuevo");

        service.ejecutar("token-abc123", "NuevaPassword123", "NuevaPassword123");

        verify(usuarioRepository).actualizarPassword("den-001", "hash-bcrypt-nuevo");
        verify(tokenRepository).actualizar(token);
        assertTrue(token.isUsado());
    }

    @Test
    @DisplayName("contraseñas no coinciden: lanza IllegalStateException y no toca ningún repositorio")
    void passwordsNoCoinciden() {
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("token-abc123", "Password123", "OtraPassword456"));

        verifyNoInteractions(tokenRepository, usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("token no existe: lanza IllegalStateException sin tocar usuarioRepository")
    void tokenNoExiste() {
        when(tokenRepository.buscarPorToken("token-no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("token-no-existe", "Password123", "Password123"));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("token expirado: lanza IllegalStateException sin actualizar password")
    void tokenExpirado() {
        TokenReseteoPassword expirado = TokenReseteoPassword.reconstituir(
            "token-viejo", "den-001",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusMinutes(90),
            false, null);
        when(tokenRepository.buscarPorToken("token-viejo")).thenReturn(Optional.of(expirado));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("token-viejo", "Password123", "Password123"));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
        verify(tokenRepository, never()).actualizar(any());
    }

    @Test
    @DisplayName("token ya usado: lanza IllegalStateException — no se puede reutilizar")
    void tokenYaUsado() {
        TokenReseteoPassword usado = TokenReseteoPassword.reconstituir(
            "token-usado", "den-001",
            LocalDateTime.now().minusMinutes(20),
            LocalDateTime.now().plusMinutes(10),
            true, LocalDateTime.now().minusMinutes(15));
        when(tokenRepository.buscarPorToken("token-usado")).thenReturn(Optional.of(usado));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("token-usado", "Password123", "Password123"));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("actualiza usuarios ANTES de marcar el token usado (orden correcto de operaciones)")
    void ordenDeOperaciones() {
        TokenReseteoPassword token = tokenVigente();
        when(tokenRepository.buscarPorToken("token-abc123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(any())).thenReturn("hash-x");

        service.ejecutar("token-abc123", "Password123", "Password123");

        var inOrder = inOrder(usuarioRepository, tokenRepository);
        inOrder.verify(usuarioRepository).actualizarPassword(any(), any());
        inOrder.verify(tokenRepository).actualizar(any());
    }
}
