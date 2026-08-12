/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.websocket;

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.in.PublicarUbicacionAgentePort;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.in.websocket.UbicacionAgenteController.UbicacionPayload;
import com.callsos.backend.infrastructure.adapter.in.websocket.UbicacionAgenteController.UbicacionResponse;
import com.callsos.backend.infrastructure.adapter.in.websocket.UbicacionAgenteController.UltimaUbicacionRequest;
import com.callsos.backend.infrastructure.adapter.out.ruta.SimulacionEstado;
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
 *
 * ACTUALIZADO tras rebase: el controller ahora recibe 4 dependencias
 * (antes 2) — PublicarUbicacionAgentePort y SimulacionEstado se agregaron
 * para soportar la simulación de recorrido GPS (pruebas piloto). La
 * lógica de "persistir + publicar en el topic STOMP" para una posición
 * REAL ya no vive en este controller — se delegó a
 * PublicarUbicacionAgenteService (ver PublicarUbicacionAgenteServiceTest),
 * así que las interacciones con repositorio/messagingTemplate para
 * recibirUbicacion() se reemplazan por verificar la llamada al puerto.
 * repositorio/messagingTemplate siguen usándose directamente SOLO en
 * solicitarUltimaPosicion(), que no cambió.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UbicacionAgenteController")
class UbicacionAgenteControllerTest {

    @Mock PublicarUbicacionAgentePort publicarUbicacion;
    @Mock SimulacionEstado simulacionEstado;
    @Mock UbicacionAgenteRepositoryPort repositorio;
    @Mock SimpMessagingTemplate messagingTemplate;

    UbicacionAgenteController controller;

    @BeforeEach
    void setUp() {
        controller = new UbicacionAgenteController(
            publicarUbicacion, simulacionEstado, repositorio, messagingTemplate);
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

        verifyNoInteractions(repositorio, messagingTemplate, publicarUbicacion);
    }

    @Test
    @DisplayName("usa el agenteId del Principal (JWT), NO el del payload — previene suplantación")
    void usaAgenteIdDelPrincipalNoDelPayload() {
        // El payload dice "ag-otro", pero el Principal autenticado es "ag-001".
        // REGRESIÓN cubierta aquí: tras el rebase, recibirUbicacion() llamaba
        // a publicarUbicacion.publicar(payload.agenteId(), ...) en vez de
        // agenteIdAutenticado — este test debe fallar si esa regresión
        // vuelve a introducirse.
        UbicacionPayload payload = new UbicacionPayload("ag-otro", 10.4, -75.5);
        Principal principal = principalAgente("ag-001");

        controller.recibirUbicacion("i-001", payload, principal);

        ArgumentCaptor<String> agenteIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(publicarUbicacion).publicar(
            agenteIdCaptor.capture(), eq("i-001"), any(Ubicacion.class));
        assertEquals("ag-001", agenteIdCaptor.getValue(),
            "Debe usar el agenteId autenticado, nunca el que declara el cliente en el payload");
    }

    @Test
    @DisplayName("delega en PublicarUbicacionAgentePort con la Ubicacion construida del payload")
    void delegaEnPuertoConUbicacionDelPayload() {
        UbicacionPayload payload = new UbicacionPayload("ag-001", 10.4, -75.5);
        Principal principal = principalAgente("ag-001");

        controller.recibirUbicacion("i-001", payload, principal);

        ArgumentCaptor<Ubicacion> ubicacionCaptor = ArgumentCaptor.forClass(Ubicacion.class);
        verify(publicarUbicacion).publicar(
            eq("ag-001"), eq("i-001"), ubicacionCaptor.capture());

        assertEquals(10.4, ubicacionCaptor.getValue().getLatitud());
        assertEquals(-75.5, ubicacionCaptor.getValue().getLongitud());
        // La persistencia y la publicación en el topic STOMP para una
        // posición real ya no ocurren en el controller — quedan a cargo de
        // PublicarUbicacionAgenteService (ver PublicarUbicacionAgenteServiceTest).
        verifyNoInteractions(repositorio, messagingTemplate);
    }

    @Test
    @DisplayName("simulación activa para el incidente: ignora la ubicación real sin tocar nada")
    void simulacionActivaIgnoraUbicacionReal() {
        UbicacionPayload payload = new UbicacionPayload("ag-001", 10.4, -75.5);
        Principal principal = principalAgente("ag-001");
        when(simulacionEstado.estaSimulando("i-001")).thenReturn(true);

        controller.recibirUbicacion("i-001", payload, principal);

        verifyNoInteractions(publicarUbicacion, repositorio, messagingTemplate);
    }

    @Test
    @DisplayName("rol distinto de AGENTE lanza IllegalStateException y no persiste ni publica")
    void rolDistintoDeAgente() {
        UbicacionPayload payload = new UbicacionPayload("den-001", 10.4, -75.5);
        Principal principal = new UsernamePasswordAuthenticationToken(
            "den-001", null, List.of(new SimpleGrantedAuthority("ROLE_DENUNCIANTE")));

        assertThrows(IllegalStateException.class,
            () -> controller.recibirUbicacion("i-001", payload, principal));

        verifyNoInteractions(repositorio, messagingTemplate, publicarUbicacion);
    }

    @Test
    @DisplayName("ubicación con latitud fuera de rango lanza excepción (Ubicacion valida rangos)")
    void ubicacionFueraDeRango() {
        UbicacionPayload payload = new UbicacionPayload("ag-001", 999.0, -75.5);
        Principal principal = principalAgente("ag-001");

        assertThrows(IllegalArgumentException.class,
            () -> controller.recibirUbicacion("i-001", payload, principal));

        verifyNoInteractions(repositorio, messagingTemplate, publicarUbicacion);
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