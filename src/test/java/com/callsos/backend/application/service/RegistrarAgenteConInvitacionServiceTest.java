/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.RolUsuario;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.domain.port.in.RegistrarAgenteConInvitacionPort.RegistroAgenteData;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.InvitacionAgenteRepositoryPort;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarAgenteConInvitacionService")
class RegistrarAgenteConInvitacionServiceTest {

    @Mock InvitacionAgenteRepositoryPort invitacionRepository;
    @Mock AgenteRepositoryPort agenteRepository;
    @Mock UsuarioRepositoryPort usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    RegistrarAgenteConInvitacionService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarAgenteConInvitacionService(
            invitacionRepository, agenteRepository, usuarioRepository,
            passwordEncoder, jwtService);
    }

    private RegistroAgenteData datosValidos(String token) {
        return new RegistroAgenteData(
            token, "Pedro Agente", "3002222222", "pedro.agente@callsos.test",
            "pedro.agente", "Password123", "Password123");
    }

    @Test
    @DisplayName("registro exitoso: guarda agente con el CAI de la invitación, marca usada y retorna JWT")
    void registroExitoso() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-001", "usr-comando-001");
        when(invitacionRepository.buscarPorToken(invitacion.getToken()))
            .thenReturn(Optional.of(invitacion));
        when(usuarioRepository.existePorUsername("pedro.agente")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash-bcrypt-xyz");
        when(jwtService.generarToken(any(), eq(RolUsuario.AGENTE.name())))
            .thenReturn("jwt-token-xyz");

        LoginPort.LoginResultado resultado = service.ejecutar(datosValidos(invitacion.getToken()));

        // El CAI viene de la invitación, nunca del cliente
        verify(agenteRepository).guardar(any(), eq("cai-001"));
        verify(usuarioRepository).guardar(
            any(), eq("pedro.agente"), eq("Pedro Agente"), eq("hash-bcrypt-xyz"),
            eq(RolUsuario.AGENTE.name()), any());
        verify(invitacionRepository).actualizar(invitacion);
        assertTrue(invitacion.isUsado());

        assertEquals("jwt-token-xyz", resultado.token());
        assertEquals(RolUsuario.AGENTE.name(), resultado.rol());
        assertEquals("Pedro Agente", resultado.nombre());
    }

    @Test
    @DisplayName(
        "registro exitoso (Épica 8, hallazgo #6, Parte 1): el agente guardado lleva el "
        + "correo del formulario — antes de este fix, Agente no tenía ningún campo de correo")
    void registroGuardaElCorreo() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-001", "usr-comando-001");
        when(invitacionRepository.buscarPorToken(invitacion.getToken()))
            .thenReturn(Optional.of(invitacion));
        when(usuarioRepository.existePorUsername("pedro.agente")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash-bcrypt-xyz");
        when(jwtService.generarToken(any(), any())).thenReturn("jwt-token-xyz");

        service.ejecutar(datosValidos(invitacion.getToken()));

        ArgumentCaptor<Agente> captor = ArgumentCaptor.forClass(Agente.class);
        verify(agenteRepository).guardar(captor.capture(), eq("cai-001"));
        assertEquals("pedro.agente@callsos.test", captor.getValue().getCorreo());
    }

    @Test
    @DisplayName("contraseñas no coinciden: lanza IllegalStateException sin tocar los repositorios")
    void passwordsNoCoinciden() {
        RegistroAgenteData datos = new RegistroAgenteData(
            "token-x", "Pedro", "300", "pedro@callsos.test", "pedro",
            "Password123", "OtraPassword456");

        assertThrows(IllegalStateException.class, () -> service.ejecutar(datos));

        verifyNoInteractions(invitacionRepository, agenteRepository, usuarioRepository, jwtService);
    }

    @Test
    @DisplayName("token inválido (no existe) lanza IllegalStateException")
    void tokenInvalido() {
        when(invitacionRepository.buscarPorToken("token-inexistente"))
            .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar(datosValidos("token-inexistente")));

        verifyNoInteractions(agenteRepository, usuarioRepository);
    }

    @Test
    @DisplayName("invitación ya usada (no vigente) lanza IllegalStateException")
    void invitacionYaUsada() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-001", "usr-comando-001");
        invitacion.marcarUsado("ag-otro-001"); // ya no está vigente
        when(invitacionRepository.buscarPorToken(invitacion.getToken()))
            .thenReturn(Optional.of(invitacion));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar(datosValidos(invitacion.getToken())));

        verifyNoInteractions(agenteRepository, usuarioRepository);
    }

    @Test
    @DisplayName("username ya existente lanza IllegalStateException y no crea nada")
    void usernameYaExiste() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-001", "usr-comando-001");
        when(invitacionRepository.buscarPorToken(invitacion.getToken()))
            .thenReturn(Optional.of(invitacion));
        when(usuarioRepository.existePorUsername("pedro.agente")).thenReturn(true);

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar(datosValidos(invitacion.getToken())));

        verify(agenteRepository, never()).guardar(any(), any());
        verify(usuarioRepository, never()).guardar(any(), any(), any(), any(), any(), any());
        verify(invitacionRepository, never()).actualizar(any());
        assertFalse(invitacion.isUsado(), "No debe marcarse usada si el registro falla después");
    }
}