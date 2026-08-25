/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.application.service.support.AgenteLiberador;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CambiarEstadoIncidenteService")
class CambiarEstadoIncidenteServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;
    @Mock EventPublisherPort eventPublisher;
    @Mock AgenteLiberador agenteLiberador;

    CambiarEstadoIncidenteService service;

    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @BeforeEach
    void setUp() {
        service = new CambiarEstadoIncidenteService(incidenteRepository, eventPublisher, agenteLiberador);
    }

    private Incidente incidenteEnEstado(EstadoIncidente estado) {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        incidente.reconstituirEstado(estado);
        return incidente;
    }

    @Test
    @DisplayName("transición válida cambia el estado y guarda")
    void transicionValida() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.DERIVADO_A_CAI);

        assertEquals(EstadoIncidente.DERIVADO_A_CAI, incidente.getEstado());
        verify(incidenteRepository).guardar(incidente);
    }

    @Test
    @DisplayName("transición NO terminal (ej. CREADO -> DERIVADO_A_CAI) NO libera al agente "
        + "— todavía no hay ninguno asignado, y aunque lo hubiera, seguiría trabajando")
    void transicionNoTerminalNoLiberaAgente() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.DERIVADO_A_CAI);

        verifyNoInteractions(agenteLiberador);
    }

    @Test
    @DisplayName("FIX agente OCUPADO para siempre: cambiar a FINALIZADO libera al agente asignado")
    void cambiarAFinalizadoLiberaAgente() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.EN_ATENCION);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.FINALIZADO);

        var inOrder = inOrder(incidenteRepository, agenteLiberador);
        inOrder.verify(incidenteRepository).guardar(incidente);
        inOrder.verify(agenteLiberador).liberarSiHayAsignacionActiva("i-001");
    }

    @Test
    @DisplayName("publica IncidenteEvent con el estadoAnterior real (Épica 2, fix P5)")
    void publicaEventoConEstadoAnteriorReal() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.DERIVADO_A_CAI);

        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        verify(eventPublisher).publicar(captor.capture());
        IncidenteEvent evento = captor.getValue();
        assertEquals("i-001", evento.getIncidenteId());
        assertEquals("den-001", evento.getDenuncianteId());
        assertEquals(EstadoIncidente.CREADO, evento.getEstadoAnterior());
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, evento.getEstadoNuevo());
    }

    @Test
    @DisplayName("cambiar a CANCELADO funciona desde cualquier estado activo y publica evento (fix P4)")
    void cambiarACancelado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_ASIGNADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.CANCELADO);

        assertEquals(EstadoIncidente.CANCELADO, incidente.getEstado());
        verify(incidenteRepository).guardar(incidente);

        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        verify(eventPublisher).publicar(captor.capture());
        assertEquals(EstadoIncidente.AGENTE_ASIGNADO, captor.getValue().getEstadoAnterior());
        assertEquals(EstadoIncidente.CANCELADO, captor.getValue().getEstadoNuevo());
    }

    @Test
    @DisplayName("FIX agente OCUPADO para siempre: CANCELADO con agente ya asignado/en camino/en "
        + "atención también lo libera — no solo el flujo de finalización normal")
    void cancelarConAgenteAsignadoLiberaAgente() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_EN_CAMINO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.CANCELADO);

        verify(agenteLiberador).liberarSiHayAsignacionActiva("i-001");
    }

    @Test
    @DisplayName("CANCELADO desde CREADO (sin agente todavía) igual intenta liberar — "
        + "AgenteLiberador es quien decide que no hay nada que hacer (no-op silencioso)")
    void cancelarSinAgenteAsignadoTambienLlamaAgenteLiberador() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.CANCELADO);

        // Se llama igual — la decisión de "no había nada que liberar" es
        // responsabilidad de AgenteLiberador (ver su propio test), no de
        // este servicio duplicando esa lógica.
        verify(agenteLiberador).liberarSiHayAsignacionActiva("i-001");
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException y no guarda ni publica")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", EstadoIncidente.DERIVADO_A_CAI));
        verify(incidenteRepository, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(agenteLiberador);
    }

    @Test
    @DisplayName("transición inválida propaga la excepción del agregado y no guarda ni publica")
    void transicionInvalida() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        // CREADO solo puede ir a DERIVADO_A_CAI (o CANCELADO) — no a EN_ATENCION
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001", EstadoIncidente.EN_ATENCION));
        verify(incidenteRepository, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(agenteLiberador);
    }
}