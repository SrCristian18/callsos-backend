/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.application.service.support.AgenteLiberador;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.IncidenteFinalizadoEvent;
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
@DisplayName("EvaluarIncidenteService")
class EvaluarIncidenteServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;
    @Mock EventPublisherPort eventPublisher;
    @Mock AgenteLiberador agenteLiberador;

    EvaluarIncidenteService service;

    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @BeforeEach
    void setUp() {
        service = new EvaluarIncidenteService(incidenteRepository, eventPublisher, agenteLiberador);
    }

    private Incidente incidenteEnEstado(EstadoIncidente estado) {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        incidente.reconstituirEstado(estado);
        return incidente;
    }

    @Test
    @DisplayName("EN_ATENCION -> FINALIZADO, guarda y publica IncidenteFinalizadoEvent")
    void finalizaYPublicaEvento() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.EN_ATENCION);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001");

        assertEquals(EstadoIncidente.FINALIZADO, incidente.getEstado());
        verify(incidenteRepository).guardar(incidente);

        ArgumentCaptor<IncidenteFinalizadoEvent> captor =
            ArgumentCaptor.forClass(IncidenteFinalizadoEvent.class);
        verify(eventPublisher).publicar(captor.capture());

        IncidenteFinalizadoEvent evento = captor.getValue();
        assertEquals("i-001", evento.getIncidenteId());
        assertEquals("den-001", evento.getDenuncianteId());
        assertEquals(EstadoIncidente.FINALIZADO, evento.getEstadoNuevo());
    }

    @Test
    @DisplayName("FIX agente OCUPADO para siempre: libera al agente asignado tras finalizar")
    void liberaAlAgenteAsignado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.EN_ATENCION);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001");

        // El orden importa: liberar DESPUÉS de persistir el incidente
        // finalizado, no antes — evita un estado intermedio inconsistente
        // si algo fallara entre medio.
        var inOrder = inOrder(incidenteRepository, agenteLiberador);
        inOrder.verify(incidenteRepository).guardar(incidente);
        inOrder.verify(agenteLiberador).liberarSiHayAsignacionActiva("i-001");
    }

    @Test
    @DisplayName("estado distinto de EN_ATENCION lanza IllegalStateException, no guarda ni publica ni libera")
    void estadoInvalidoNoPermiteEvaluar() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_EN_CAMINO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(IllegalStateException.class, () -> service.ejecutar("i-001"));

        assertEquals(EstadoIncidente.AGENTE_EN_CAMINO, incidente.getEstado());
        verify(incidenteRepository, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(agenteLiberador);
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar("no-existe"));
        verify(incidenteRepository, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(agenteLiberador);
    }
}