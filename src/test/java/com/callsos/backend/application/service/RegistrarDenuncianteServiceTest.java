/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.RolUsuario;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.domain.port.in.RegistrarDenunciantePort.RegistroDenuncianteData;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import com.callsos.backend.infrastructure.config.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarDenuncianteService")
class RegistrarDenuncianteServiceTest {

    @Mock DenuncianteRepositoryPort denuncianteRepository;
    @Mock UsuarioRepositoryPort usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    RegistrarDenuncianteService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarDenuncianteService(
            denuncianteRepository, usuarioRepository, passwordEncoder, jwtService);
    }

    private RegistroDenuncianteData datosValidos() {
        return new RegistroDenuncianteData(
            "Juan", "Pérez", "1001234567", "3001111111",
            "Password123", "Password123");
    }

    @Test
    @DisplayName("registro exitoso: guarda denunciante y usuario, autologuea con JWT")
    void registroExitoso() {
        when(denuncianteRepository.existePorDocumento("1001234567")).thenReturn(false);
        when(usuarioRepository.existePorUsername("1001234567")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash-bcrypt-xyz");
        when(jwtService.generarToken(any(), eq(RolUsuario.DENUNCIANTE.name())))
            .thenReturn("jwt-token-xyz");

        LoginPort.LoginResultado resultado = service.ejecutar(datosValidos());

        ArgumentCaptor<Denunciante> captor = ArgumentCaptor.forClass(Denunciante.class);
        verify(denuncianteRepository).guardar(captor.capture());
        assertEquals("Juan Pérez", captor.getValue().getNombre());
        assertEquals("1001234567", captor.getValue().getDocumento());

        verify(usuarioRepository).guardar(
            any(), eq("1001234567"), eq("Juan Pérez"), eq("hash-bcrypt-xyz"),
            eq(RolUsuario.DENUNCIANTE.name()), any());

        assertEquals("jwt-token-xyz", resultado.token());
        assertEquals(RolUsuario.DENUNCIANTE.name(), resultado.rol());
        assertEquals("Juan Pérez", resultado.nombre());
    }

    @Test
    @DisplayName("contraseñas no coinciden lanza IllegalStateException sin tocar repositorios")
    void passwordsNoCoinciden() {
        RegistroDenuncianteData datos = new RegistroDenuncianteData(
            "Juan", "Pérez", "1001234567", "300", "Password123", "OtraPassword456");

        assertThrows(IllegalStateException.class, () -> service.ejecutar(datos));

        verifyNoInteractions(denuncianteRepository, usuarioRepository, jwtService);
    }

    @Test
    @DisplayName("documento en blanco lanza IllegalStateException")
    void documentoEnBlanco() {
        RegistroDenuncianteData datos = new RegistroDenuncianteData(
            "Juan", "Pérez", "   ", "300", "Password123", "Password123");

        assertThrows(IllegalStateException.class, () -> service.ejecutar(datos));

        verifyNoInteractions(denuncianteRepository, usuarioRepository, jwtService);
    }

    @Test
    @DisplayName("documento null lanza IllegalStateException")
    void documentoNulo() {
        RegistroDenuncianteData datos = new RegistroDenuncianteData(
            "Juan", "Pérez", null, "300", "Password123", "Password123");

        assertThrows(IllegalStateException.class, () -> service.ejecutar(datos));

        verifyNoInteractions(denuncianteRepository, usuarioRepository, jwtService);
    }

    @Test
    @DisplayName("documento ya registrado lanza IllegalStateException y no crea usuario")
    void documentoYaExiste() {
        when(denuncianteRepository.existePorDocumento("1001234567")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.ejecutar(datosValidos()));

        verify(denuncianteRepository, never()).guardar(any());
        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    @DisplayName("username (documento) ya existente en usuarios lanza IllegalStateException")
    void usernameYaExiste() {
        when(denuncianteRepository.existePorDocumento("1001234567")).thenReturn(false);
        when(usuarioRepository.existePorUsername("1001234567")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.ejecutar(datosValidos()));

        verify(denuncianteRepository, never()).guardar(any());
        verify(usuarioRepository, never()).guardar(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(jwtService);
    }
}
