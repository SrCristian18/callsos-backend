/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.event;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.AgenteEnCaminoEvent;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Épica 2 — cubre:
 *  - fix P4: eventos de transiciones "nuevas" (más allá de las 2 que ya
 *    disparaban algo) también quedan registrados — se prueba acá con
 *    IncidenteEvent genérico y AgenteEnCaminoEvent.
 *  - fix P5: estadoAnterior real se propaga hasta el registro.
 *  - TipoIncidenteActualizadoEvent se registra como cambio de campo
 *    genérico y NO además como una (falsa) transición de estado — es
 *    el caso que exige el instanceof-guard en onCambioEstado().
 *  - actor "sistema"/SISTEMA cuando no hay autenticación en el contexto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaEventListener")
class AuditoriaEventListenerTest {

    @Mock AuditoriaRepositoryPort auditoriaRepository;

    AuditoriaEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuditoriaEventListener(auditoriaRepository);
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String actorId, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                actorId, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @Test
    @DisplayName("onCambioEstado registra un IncidenteEvent genérico con estadoAnterior real (fix P4 + P5)")
    void registraTransicionGenerica() {
        autenticarComo("usr-comando-001", "COMANDO");

        IncidenteEvent evento = new IncidenteEvent(
            "i-001", "den-001", EstadoIncidente.CREADO, EstadoIncidente.DERIVADO_A_CAI);

        listener.onCambioEstado(evento);

        ArgumentCaptor<AuditoriaIncidente> captor = ArgumentCaptor.forClass(AuditoriaIncidente.class);
        verify(auditoriaRepository).registrar(captor.capture());

        AuditoriaIncidente registrado = captor.getValue();
        assertEquals("i-001", registrado.getIncidenteId());
        assertEquals(EstadoIncidente.CREADO, registrado.getEstadoAnterior());
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, registrado.getEstadoNuevo());
        assertEquals("usr-comando-001", registrado.getActorId());
        assertEquals("COMANDO", registrado.getActorRol());
        assertFalse(registrado.esCambioGenerico());
    }

    @Test
    @DisplayName("onCambioEstado también captura subclases como AgenteEnCaminoEvent")
    void registraSubclaseDeIncidenteEvent() {
        autenticarComo("ag-001", "AGENTE");

        AgenteEnCaminoEvent evento = new AgenteEnCaminoEvent(
            "i-002", "den-002", EstadoIncidente.AGENTE_ASIGNADO, "ag-001");

        listener.onCambioEstado(evento);

        ArgumentCaptor<AuditoriaIncidente> captor = ArgumentCaptor.forClass(AuditoriaIncidente.class);
        verify(auditoriaRepository).registrar(captor.capture());
        assertEquals(EstadoIncidente.AGENTE_ASIGNADO, captor.getValue().getEstadoAnterior());
        assertEquals(EstadoIncidente.AGENTE_EN_CAMINO, captor.getValue().getEstadoNuevo());
    }

    @Test
    @DisplayName("onCambioEstado ignora TipoIncidenteActualizadoEvent (lo maneja onCambioTipo, no duplica)")
    void noDuplicaCambioDeTipoEnOnCambioEstado() {
        TipoIncidenteActualizadoEvent evento = new TipoIncidenteActualizadoEvent(
            "i-003", "den-003", EstadoIncidente.CREADO,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS);

        listener.onCambioEstado(evento);

        verifyNoInteractions(auditoriaRepository);
    }

    @Test
    @DisplayName("onCambioTipo registra un cambio de campo genérico, no una transición de estado")
    void registraCambioDeTipoComoGenerico() {
        autenticarComo("den-003", "DENUNCIANTE");

        TipoIncidenteActualizadoEvent evento = new TipoIncidenteActualizadoEvent(
            "i-003", "den-003", EstadoIncidente.CREADO,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS);

        listener.onCambioTipo(evento);

        ArgumentCaptor<AuditoriaIncidente> captor = ArgumentCaptor.forClass(AuditoriaIncidente.class);
        verify(auditoriaRepository).registrar(captor.capture());

        AuditoriaIncidente registrado = captor.getValue();
        assertNull(registrado.getEstadoAnterior(),
            "El cambio de tipo no es una transición de estado");
        assertEquals(EstadoIncidente.CREADO, registrado.getEstadoNuevo());
        assertEquals("tipo", registrado.getCampo());
        assertEquals("ROBOS_O_ASALTOS", registrado.getValorAnteriorGenerico());
        assertEquals("RIÑAS_O_PELEAS", registrado.getValorNuevoGenerico());
        assertEquals("den-003", registrado.getActorId());
        assertEquals("DENUNCIANTE", registrado.getActorRol());
        assertTrue(registrado.esCambioGenerico());
    }

    @Test
    @DisplayName("sin autenticación en el contexto, el actor queda como sistema/SISTEMA")
    void actorSistemaSinAutenticacion() {
        // SecurityContextHolder vacío (limpio por @AfterEach del test anterior)
        IncidenteEvent evento = new IncidenteEvent(
            "i-004", "den-004", null, EstadoIncidente.CREADO);

        listener.onCambioEstado(evento);

        ArgumentCaptor<AuditoriaIncidente> captor = ArgumentCaptor.forClass(AuditoriaIncidente.class);
        verify(auditoriaRepository).registrar(captor.capture());
        assertEquals("sistema", captor.getValue().getActorId());
        assertEquals("SISTEMA", captor.getValue().getActorRol());
    }
}
