/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.TokenReseteoPassword;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.EnviarCorreoPort;
import com.callsos.backend.domain.port.out.TokenReseteoPasswordRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Épica 8 (hallazgo #6, Parte 2).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitarReseteoPasswordService")
class SolicitarReseteoPasswordServiceTest {

    @Mock DenuncianteRepositoryPort denuncianteRepository;
    @Mock AgenteRepositoryPort agenteRepository;
    @Mock UnidadPolicialRepositoryPort unidadPolicialRepository;
    @Mock TokenReseteoPasswordRepositoryPort tokenRepository;
    @Mock EnviarCorreoPort enviarCorreo;

    SolicitarReseteoPasswordService service;

    @BeforeEach
    void setUp() {
        service = new SolicitarReseteoPasswordService(
            denuncianteRepository, agenteRepository, unidadPolicialRepository,
            tokenRepository, enviarCorreo);
    }

    @Test
    @DisplayName("correo de un DENUNCIANTE: genera token con su actorId y envía el correo")
    void correoDeDenunciante() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Pérez", "1001", "APP", "300",
            "juan.perez@callsos.test", null);
        when(denuncianteRepository.buscarPorCorreo("juan.perez@callsos.test"))
            .thenReturn(Optional.of(denunciante));

        service.ejecutar("juan.perez@callsos.test");

        ArgumentCaptor<TokenReseteoPassword> captor =
            ArgumentCaptor.forClass(TokenReseteoPassword.class);
        verify(tokenRepository).guardar(captor.capture());
        assertEquals("den-001", captor.getValue().getActorId());
        assertTrue(captor.getValue().estaVigente());

        verify(enviarCorreo).enviar(
            eq("juan.perez@callsos.test"), any(), any());
        verifyNoInteractions(agenteRepository, unidadPolicialRepository);
    }

    @Test
    @DisplayName("correo de un AGENTE (no encontrado como denunciante): genera token con su actorId")
    void correoDeAgente() {
        when(denuncianteRepository.buscarPorCorreo("pedro.agente@callsos.test"))
            .thenReturn(Optional.empty());
        Agente agente = new Agente(
            "ag-001", "Pedro Agente", "Dir", new Ubicacion(10.4, -75.5), "300");
        agente.setCorreo("pedro.agente@callsos.test");
        when(agenteRepository.buscarPorCorreo("pedro.agente@callsos.test"))
            .thenReturn(Optional.of(agente));

        service.ejecutar("pedro.agente@callsos.test");

        ArgumentCaptor<TokenReseteoPassword> captor =
            ArgumentCaptor.forClass(TokenReseteoPassword.class);
        verify(tokenRepository).guardar(captor.capture());
        assertEquals("ag-001", captor.getValue().getActorId());

        verify(enviarCorreo).enviar(eq("pedro.agente@callsos.test"), any(), any());
        verifyNoInteractions(unidadPolicialRepository);
    }

    @Test
    @DisplayName("correo de un CAI (no encontrado como denunciante ni agente): genera token con su actorId")
    void correoDeCai() {
        when(denuncianteRepository.buscarPorCorreo("cai.central@callsos.test"))
            .thenReturn(Optional.empty());
        when(agenteRepository.buscarPorCorreo("cai.central@callsos.test"))
            .thenReturn(Optional.empty());
        UnidadPolicial cai = new UnidadPolicial(
            "cai-001", "CAI Central", "Dir", new Ubicacion(10.4, -75.5), "601");
        cai.setCorreo("cai.central@callsos.test");
        when(unidadPolicialRepository.buscarPorCorreo("cai.central@callsos.test"))
            .thenReturn(Optional.of(cai));

        service.ejecutar("cai.central@callsos.test");

        ArgumentCaptor<TokenReseteoPassword> captor =
            ArgumentCaptor.forClass(TokenReseteoPassword.class);
        verify(tokenRepository).guardar(captor.capture());
        assertEquals("cai-001", captor.getValue().getActorId());

        verify(enviarCorreo).enviar(eq("cai.central@callsos.test"), any(), any());
    }

    @Test
    @DisplayName(
        "correo que no pertenece a nadie: NO genera token ni envía correo, y NO lanza "
        + "excepción (anti-enumeración de cuentas)")
    void correoInexistente() {
        when(denuncianteRepository.buscarPorCorreo(any())).thenReturn(Optional.empty());
        when(agenteRepository.buscarPorCorreo(any())).thenReturn(Optional.empty());
        when(unidadPolicialRepository.buscarPorCorreo(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.ejecutar("no-existe@callsos.test"));

        verifyNoInteractions(tokenRepository, enviarCorreo);
    }

    @Test
    @DisplayName("correo null o en blanco: no hace nada, no lanza excepción")
    void correoNuloOEnBlanco() {
        assertDoesNotThrow(() -> service.ejecutar(null));
        assertDoesNotThrow(() -> service.ejecutar("   "));

        verifyNoInteractions(denuncianteRepository, agenteRepository,
            unidadPolicialRepository, tokenRepository, enviarCorreo);
    }

    @Test
    @DisplayName("el correo enviado incluye el token generado y la duración de vigencia")
    void correoIncluyeTokenYDuracion() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Pérez", "1001", "APP", "300",
            "juan.perez@callsos.test", null);
        when(denuncianteRepository.buscarPorCorreo("juan.perez@callsos.test"))
            .thenReturn(Optional.of(denunciante));

        ArgumentCaptor<TokenReseteoPassword> tokenCaptor =
            ArgumentCaptor.forClass(TokenReseteoPassword.class);
        ArgumentCaptor<String> cuerpoCaptor = ArgumentCaptor.forClass(String.class);

        service.ejecutar("juan.perez@callsos.test");

        verify(tokenRepository).guardar(tokenCaptor.capture());
        verify(enviarCorreo).enviar(any(), any(), cuerpoCaptor.capture());

        String tokenGenerado = tokenCaptor.getValue().getToken();
        assertTrue(cuerpoCaptor.getValue().contains(tokenGenerado));
        assertTrue(cuerpoCaptor.getValue().contains("30"));
    }
}