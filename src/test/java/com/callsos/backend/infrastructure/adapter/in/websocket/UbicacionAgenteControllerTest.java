/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.websocket;

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.websocket.UbicacionAgenteController.UbicacionPayload;
import com.callsos.backend.infrastructure.adapter.in.websocket.UbicacionAgenteController.UbicacionResponse;
import com.callsos.backend.infrastructure.adapter.in.websocket.UbicacionAgenteController.UltimaUbicacionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Épica 4 (ruta técnica) — "Test de UbicacionAgenteController (WebSocket)".
 *
 * No es un @WebMvcTest ni requiere un broker STOMP real: recibirUbicacion()
 * y solicitarUltimaPosicion() son métodos Java normales invocados por
 * Spring al llegar un frame STOMP — se prueban invocándolos directamente,
 * igual que se hizo con JwtAuthFilter.doFilterInternal().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UbicacionAgenteController")
class UbicacionAgenteControllerTest {

    @Mock UbicacionAgenteRepositoryPort repositorio;
    @Mock SimpMessagingTemplate messagingTemplate;

    UbicacionAgenteController controller;

    @BeforeEach
    void setUp() {
        controller = new UbicacionAgenteController(repositorio, messagingTemplate);
    }

    private UsernamePasswordAuthenticationToken principalAgente(String agenteId) {
        return new UsernamePasswordAuthenticationToken(
            agenteId, null, List.of(new SimpleGrantedAuthority("ROLE_AGENTE")));
    }

    @Test
    @DisplayName("principal null lanza IllegalStateException y no persiste ni publica")
    void principalNulo() {
        UbicacionPayload payload = new UbicacionPayload("ag-otro", 10.4, -75.5);

        assertThrows(IllegalStateException.class,
            () -> controller.recibirUbicacion("i-001", payload, null));

        verifyNoInteractions(repositorio, messagingTemplate);
    }

    @Test
    @DisplayName("usa el agenteId del Principal (JWT), NO el del payload — previene suplantación")
    void usaAgenteIdDelPrincipalNoDelPayload() {
        // El payload dice "ag-otro", pero el Principal autenticado es "ag-001"
        UbicacionPayload payload = new UbicacionPayload("ag-otro", 10.4, -75.5);
        Principal principal = principalAgente("ag-001");

        controller.recibirUbicacion("i-001", payload, principal);

        ArgumentCaptor<UbicacionAgente> captor = ArgumentCaptor.forClass(UbicacionAgente.class);
        verify(repositorio).guardar(captor.capture());
        assertEquals("ag-001", captor.getValue().getAgenteId(),
            "Debe usar el agenteId autenticado, nunca el que declara el cliente en el payload");
        assertEquals("i-001", captor.getValue().getIncidenteId());
    }

    @Test
    @DisplayName("publica en /topic/incidente/{id}/ubicacion con lat/lon del payload")
    void publicaEnElTopicoCorrecto() {
        UbicacionPayload payload = new UbicacionPayload("ag-001", 10.4, -75.5);
        Principal principal = principalAgente("ag-001");

        controller.recibirUbicacion("i-001", payload, principal);

        ArgumentCaptor<UbicacionResponse> captor = ArgumentCaptor.forClass(UbicacionResponse.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/incidente/i-001/ubicacion"), captor.capture());

        assertEquals(10.4, captor.getValue().latitud());
        assertEquals(-75.5, captor.getValue().longitud());
        assertNotNull(captor.getValue().timestamp());
    }

    @Test
    @DisplayName("rol distinto de AGENTE lanza IllegalStateException y no persiste ni publica")
    void rolDistintoDeAgente() {
        UbicacionPayload payload = new UbicacionPayload("den-001", 10.4, -75.5);
        Principal principal = new UsernamePasswordAuthenticationToken(
            "den-001", null, List.of(new SimpleGrantedAuthority("ROLE_DENUNCIANTE")));

        assertThrows(IllegalStateException.class,
            () -> controller.recibirUbicacion("i-001", payload, principal));

        verifyNoInteractions(repositorio, messagingTemplate);
    }

    @Test
    @DisplayName("ubicación con latitud fuera de rango lanza excepción (Ubicacion valida rangos)")
    void ubicacionFueraDeRango() {
        UbicacionPayload payload = new UbicacionPayload("ag-001", 999.0, -75.5);
        Principal principal = principalAgente("ag-001");

        assertThrows(IllegalArgumentException.class,
            () -> controller.recibirUbicacion("i-001", payload, principal));

        verifyNoInteractions(repositorio, messagingTemplate);
    }

    @Test
    @DisplayName("solicitarUltimaPosicion publica la última posición si existe")
    void solicitarUltimaPosicionExistente() {
        UbicacionAgente ultima = new UbicacionAgente(
            "ag-001", "i-001", new Ubicacion(10.4, -75.5));
        when(repositorio.ultimaPosicion("ag-001", "i-001")).thenReturn(Optional.of(ultima));

        controller.solicitarUltimaPosicion("i-001", new UltimaUbicacionRequest("ag-001"));

        verify(messagingTemplate).convertAndSend(
            eq("/topic/incidente/i-001/ubicacion"), any(UbicacionResponse.class));
    }

    @Test
    @DisplayName("solicitarUltimaPosicion no publica nada si no hay historial")
    void solicitarUltimaPosicionInexistente() {
        when(repositorio.ultimaPosicion("ag-001", "i-001")).thenReturn(Optional.empty());

        controller.solicitarUltimaPosicion("i-001", new UltimaUbicacionRequest("ag-001"));

        verifyNoInteractions(messagingTemplate);
    }
}
