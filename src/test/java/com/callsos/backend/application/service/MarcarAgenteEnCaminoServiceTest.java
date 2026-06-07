/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.AgenteEnCaminoEvent;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
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
 
import java.time.LocalDateTime;
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("MarcarAgenteEnCaminoService")
public class MarcarAgenteEnCaminoServiceTest {
    
    @Mock IncidenteRepositoryPort incidenteRepo;
    @Mock AsignacionRepositoryPort asignacionRepo;
    @Mock EventPublisherPort eventPublisher;
 
    private MarcarAgenteEnCaminoService service;
 
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "d-001", "Juan", "Cartagena", "300", "j@test.com");
 
    @BeforeEach
    void setUp() {
        service = new MarcarAgenteEnCaminoService(
            incidenteRepo, asignacionRepo, eventPublisher);
    }
 
    @Test
    @DisplayName("Transiciona a AGENTE_EN_CAMINO y publica evento")
    void transicionaYPublicaEvento() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_ASIGNADO);
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepo.buscarPorIncidente("i-001")).thenReturn(Optional.empty());
 
        service.ejecutar("i-001");
 
        assertEquals(EstadoIncidente.AGENTE_EN_CAMINO, incidente.getEstado());
        verify(incidenteRepo).guardar(incidente);
        verify(eventPublisher).publicar(any(AgenteEnCaminoEvent.class));
    }
 
    @Test
    @DisplayName("El evento publicado lleva el ID correcto del incidente y denunciante")
    void eventoConDatosCorrectos() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_ASIGNADO);
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepo.buscarPorIncidente("i-001")).thenReturn(Optional.empty());
 
        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        service.ejecutar("i-001");
        verify(eventPublisher).publicar(captor.capture());
 
        IncidenteEvent evento = captor.getValue();
        assertEquals("i-001", evento.getIncidenteId());
        assertEquals("d-001", evento.getDenuncianteId());
        assertEquals(EstadoIncidente.AGENTE_EN_CAMINO, evento.getEstadoNuevo());
    }
 
    @Test
    @DisplayName("Incluye agenteId del asignacionRepo si existe asignacion activa")
    void incluyeAgenteIdDelRepo() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_ASIGNADO);
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");
        agente.asignar(); // estado OCUPADO como viene de BD
 
        Denuncia denuncia = new Denuncia(
            "den-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante, incidente);
        Asignacion asignacion = Asignacion.reconstituir(
            "as-001", LocalDateTime.now(),
            com.callsos.backend.domain.enums.EstadoAsignacion.ACTIVA, agente, denuncia);
 
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepo.buscarPorIncidente("i-001")).thenReturn(Optional.of(asignacion));
 
        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        service.ejecutar("i-001");
        verify(eventPublisher).publicar(captor.capture());
 
        AgenteEnCaminoEvent evento = (AgenteEnCaminoEvent) captor.getValue();
        assertEquals("ag-001", evento.getAgenteId());
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no existe")
    void lanzaSiIncidenteNoExiste() {
        when(incidenteRepo.buscarPorId("no-existe")).thenReturn(Optional.empty());
 
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe"));
 
        verifyNoInteractions(eventPublisher);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no esta en AGENTE_ASIGNADO")
    void lanzaSiEstadoIncorrecto() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
 
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));
 
        verifyNoInteractions(eventPublisher);
    }
 
    // ── Helper ─────────────────────────────────────────────────────────────
 
    private Incidente incidenteEnEstado(EstadoIncidente objetivo) {
        Incidente i = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        if (objetivo == EstadoIncidente.CREADO) return i;
        i.derivarACAI(new UnidadPolicial("cai-001", "CAI", "Dir", ubicacion, "600"));
        if (objetivo == EstadoIncidente.DERIVADO_A_CAI) return i;
        i.marcarAgenteAsignado();
        return i; // AGENTE_ASIGNADO
    }
}
