/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

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
@DisplayName("AtenderIncidenteService")
class AtenderIncidenteServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;
    @Mock EventPublisherPort eventPublisher;

    AtenderIncidenteService service;

    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @BeforeEach
    void setUp() {
        service = new AtenderIncidenteService(incidenteRepository, eventPublisher);
    }

    private Incidente incidenteEnEstado(EstadoIncidente estado) {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        incidente.reconstituirEstado(estado);
        return incidente;
    }

    @Test
    @DisplayName("transiciona AGENTE_EN_CAMINO -> EN_ATENCION y guarda")
    void transicionValida() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_EN_CAMINO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001");

        assertEquals(EstadoIncidente.EN_ATENCION, incidente.getEstado());
        verify(incidenteRepository).guardar(incidente);
    }

    @Test
    @DisplayName("publica IncidenteEvent con estadoAnterior AGENTE_EN_CAMINO y estadoNuevo EN_ATENCION (Épica 2)")
    void publicaEventoConEstadoAnteriorReal() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_EN_CAMINO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001");

        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        verify(eventPublisher).publicar(captor.capture());
        IncidenteEvent evento = captor.getValue();
        assertEquals("i-001", evento.getIncidenteId());
        assertEquals("den-001", evento.getDenuncianteId());
        assertEquals(EstadoIncidente.AGENTE_EN_CAMINO, evento.getEstadoAnterior());
        assertEquals(EstadoIncidente.EN_ATENCION, evento.getEstadoNuevo());
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException y no guarda")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar("no-existe"));
        verify(incidenteRepository, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("transición inválida (estado CREADO) propaga la excepción del agregado y no guarda ni publica")
    void transicionInvalida() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(IllegalStateException.class, () -> service.ejecutar("i-001"));
        verify(incidenteRepository, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
    }
}
