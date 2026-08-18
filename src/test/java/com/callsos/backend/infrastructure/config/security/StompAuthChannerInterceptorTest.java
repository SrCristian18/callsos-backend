/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

import com.callsos.backend.domain.port.in.VerificarAccesoTrackingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Cubre StompAuthChannelInterceptor — Épica 3 (fix P6).
 *
 * Foco de este test: la autorización de SUBSCRIBE sobre
 * "/topic/agente/{agenteId}/ubicacion", que es el núcleo de la Épica 3.
 * La autenticación en CONNECT ya existía antes de esta épica y se cubre
 * aquí solo para verificar que no se rompió con el cambio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StompAuthChannelInterceptor — autorización SUBSCRIBE (Épica 3)")
class StompAuthChannelInterceptorTest {

    @Mock JwtService jwtService;
    @Mock VerificarAccesoTrackingPort verificarAccesoTracking;

    StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtService, verificarAccesoTracking);
    }

    private Principal principal(String actorId, String rol) {
        return new UsernamePasswordAuthenticationToken(
            actorId, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Message<byte[]> mensajeSubscribe(String destino, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        if (principal != null) accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // ── SUBSCRIBE al topic de tracking — el núcleo de la Épica 3 ────────────

    @Test
    @DisplayName("DENUNCIANTE es rechazado al suscribirse al topic de ubicación del agente — SIEMPRE")
    void denuncianteRechazadoAlSuscribirse() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/agente/ag-001/ubicacion", principal("den-001", "DENUNCIANTE"));
        when(verificarAccesoTracking.puedeAcceder("ag-001", "den-001", "DENUNCIANTE"))
            .thenReturn(false);

        assertThrows(MessagingException.class,
            () -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("AGENTE se suscribe a su PROPIO topic de ubicación — permitido")
    void agentePropioPermitido() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/agente/ag-001/ubicacion", principal("ag-001", "AGENTE"));
        when(verificarAccesoTracking.puedeAcceder("ag-001", "ag-001", "AGENTE"))
            .thenReturn(true);

        assertDoesNotThrow(() -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("AGENTE A intenta suscribirse al topic del AGENTE B — rechazado")
    void agenteAjenoRechazado() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/agente/ag-002/ubicacion", principal("ag-001", "AGENTE"));
        when(verificarAccesoTracking.puedeAcceder("ag-002", "ag-001", "AGENTE"))
            .thenReturn(false);

        assertThrows(MessagingException.class,
            () -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("CAI A intenta suscribirse a un agente de CAI B — rechazado")
    void caiDeOtraUnidadRechazado() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/agente/ag-999/ubicacion", principal("cai-001", "OPERADOR_CAI"));
        when(verificarAccesoTracking.puedeAcceder("ag-999", "cai-001", "OPERADOR_CAI"))
            .thenReturn(false);

        assertThrows(MessagingException.class,
            () -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("CAI puede suscribirse a un agente de SU propia unidad — permitido")
    void caiDeSuPropiaUnidadPermitido() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/agente/ag-001/ubicacion", principal("cai-001", "OPERADOR_CAI"));
        when(verificarAccesoTracking.puedeAcceder("ag-001", "cai-001", "OPERADOR_CAI"))
            .thenReturn(true);

        assertDoesNotThrow(() -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("COMANDO puede suscribirse al topic de cualquier agente — permitido")
    void comandoSiemprePermitido() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/agente/ag-001/ubicacion", principal("usr-comando", "COMANDO"));
        when(verificarAccesoTracking.puedeAcceder("ag-001", "usr-comando", "COMANDO"))
            .thenReturn(true);

        assertDoesNotThrow(() -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("SUBSCRIBE sin Principal (sesión no autenticada) es rechazado")
    void subscribeSinPrincipalRechazado() {
        Message<byte[]> mensaje = mensajeSubscribe("/topic/agente/ag-001/ubicacion", null);

        assertThrows(MessagingException.class,
            () -> interceptor.preSend(mensaje, null));
        verifyNoInteractions(verificarAccesoTracking);
    }

    @Test
    @DisplayName("SUBSCRIBE a un topic que NO es de tracking pasa sin restricción (fuera de alcance de la Épica 3)")
    void subscribeATopicNoRelacionadoPasaLibre() {
        Message<byte[]> mensaje = mensajeSubscribe(
            "/topic/otro-canal-cualquiera", principal("den-001", "DENUNCIANTE"));

        assertDoesNotThrow(() -> interceptor.preSend(mensaje, null));
        verifyNoInteractions(verificarAccesoTracking);
    }

    // ── CONNECT (autenticación — regresión, ya existía antes de la Épica 3) ─

    @Test
    @DisplayName("CONNECT sin header Authorization es rechazado")
    void connectSinAuthorizationRechazado() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> mensaje = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessagingException.class,
            () -> interceptor.preSend(mensaje, null));
    }

    @Test
    @DisplayName("CONNECT con JWT válido autentica y setea el Principal en la sesión STOMP")
    void connectConJwtValidoAutentica() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-valido");
        accessor.setLeaveMutable(true);
        Message<byte[]> mensaje = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.esValido("token-valido")).thenReturn(true);
        when(jwtService.extraerUserId("token-valido")).thenReturn("ag-001");
        when(jwtService.extraerRol("token-valido")).thenReturn("AGENTE");

        interceptor.preSend(mensaje, null);

        assertNotNull(accessor.getUser());
        assertEquals("ag-001", accessor.getUser().getName());
    }

    @Test
    @DisplayName("CONNECT con JWT inválido/expirado es rechazado")
    void connectConJwtInvalidoRechazado() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-vencido");
        accessor.setLeaveMutable(true);
        Message<byte[]> mensaje = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.esValido("token-vencido")).thenReturn(false);

        assertThrows(MessagingException.class,
            () -> interceptor.preSend(mensaje, null));
    }
}
