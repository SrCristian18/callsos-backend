/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.in.LoginPort.LoginResultado;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort.UsuarioCredencial;
import com.callsos.backend.infrastructure.config.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
 
/**
 * Tests de LoginService.
 *
 * JwtService y PasswordEncoder se mockean — son infraestructura externa.
 * El dominio del caso de uso (orquestación y mensajes de error) se verifica
 * sin depender del algoritmo real de cifrado ni de la firma JWT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService")
public class LoginServiceTest {
    
    @Mock UsuarioRepositoryPort usuarioRepo;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
 
    private LoginService service;
 
    @BeforeEach
    void setUp() {
        service = new LoginService(usuarioRepo, jwtService, passwordEncoder);
    }
 
    @Test
    @DisplayName("Login exitoso retorna token, actorId y rol")
    void loginExitoso() {
        UsuarioCredencial credencial = new UsuarioCredencial(
            "usr-001", "juan.test", "Juan Pérez", "$2a$10$hash", "DENUNCIANTE", "den-001");
 
        when(usuarioRepo.buscarPorUsername("juan.test"))
            .thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("password123", "$2a$10$hash"))
            .thenReturn(true);
        when(jwtService.generarToken("den-001", "DENUNCIANTE"))
            .thenReturn("eyJhbGciOiJIUzI1NiJ9.token");
 
        LoginResultado resultado = service.ejecutar("juan.test", "password123");
 
        assertEquals("eyJhbGciOiJIUzI1NiJ9.token", resultado.token());
        assertEquals("den-001", resultado.actorId());
        assertEquals("DENUNCIANTE", resultado.rol());
        assertEquals("Juan Pérez", resultado.nombre());
 
        verify(jwtService).generarToken("den-001", "DENUNCIANTE");
    }
 
    @Test
    @DisplayName("Usuario inexistente lanza excepcion con mensaje generico")
    void usuarioNoExiste() {
        when(usuarioRepo.buscarPorUsername("no-existe"))
            .thenReturn(Optional.empty());
 
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "cualquierPass"));
 
        assertEquals("Username o contraseña incorrectos.", ex.getMessage());
        verifyNoInteractions(passwordEncoder, jwtService);
    }
 
    @Test
    @DisplayName("Contraseña incorrecta lanza excepcion con el mismo mensaje generico")
    void contrasenaIncorrecta() {
        UsuarioCredencial credencial = new UsuarioCredencial(
            "usr-001", "juan.test", "Juan Pérez", "$2a$10$hash", "DENUNCIANTE", "den-001");
 
        when(usuarioRepo.buscarPorUsername("juan.test"))
            .thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("wrongPass", "$2a$10$hash"))
            .thenReturn(false);
 
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.ejecutar("juan.test", "wrongPass"));
 
        // Mismo mensaje para no revelar si el username existe
        assertEquals("Username o contraseña incorrectos.", ex.getMessage());
        verifyNoInteractions(jwtService);
    }
 
    @Test
    @DisplayName("El JWT usa actorId como subject, no el ID interno de usuarios")
    void jwtUsaActorIdNoUsuarioId() {
        UsuarioCredencial credencial = new UsuarioCredencial(
            "usr-interno-999", "juan.test", "Juan Pérez", "$2a$10$hash", "AGENTE", "ag-real-001");
 
        when(usuarioRepo.buscarPorUsername("juan.test"))
            .thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("pass", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generarToken("ag-real-001", "AGENTE")).thenReturn("token");
 
        service.ejecutar("juan.test", "pass");
 
        // Verifica que el JWT lleva ag-real-001 (actor), no usr-interno-999
        verify(jwtService).generarToken("ag-real-001", "AGENTE");
        verify(jwtService, never()).generarToken(eq("usr-interno-999"), any());
    }
}