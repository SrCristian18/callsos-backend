/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
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
 
import java.util.List;
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("AsignarAgenteService")
public class AsignarAgenteServiceTest {
    
    @Mock IncidenteRepositoryPort incidenteRepo;
    @Mock AgenteRepositoryPort agenteRepo;
    @Mock AsignacionRepositoryPort asignacionRepo;
    @Mock EventPublisherPort eventPublisher;
 
    private AsignarAgenteService service;
 
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "d-001", "Juan", "Cartagena", "300", "j@test.com");
 
    @BeforeEach
    void setUp() {
        service = new AsignarAgenteService(agenteRepo, incidenteRepo, asignacionRepo, eventPublisher);
    }
 
    @Test
    @DisplayName("Asigna agente disponible y persiste asignacion e incidente")
    void asignaAgenteYPersisteTodo() {
        Incidente incidente = incidenteConCAIyDenuncia();
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");

        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001")).thenReturn(List.of(agente));
        when(agenteRepo.intentarReservar("ag-001")).thenReturn(true);

        service.ejecutar("i-001");

        // FIX (condición de carrera): ya no se llama actualizarEstado() al
        // final — el estado del agente se persiste atómicamente dentro de
        // intentarReservar(), verificado abajo.
        verify(agenteRepo, times(1)).intentarReservar("ag-001");
        verify(asignacionRepo, times(1)).guardar(any());
        verify(incidenteRepo, times(1)).guardar(incidente);
        verify(agenteRepo, never()).actualizarEstado(any());
    }

    @Test
    @DisplayName("Publica IncidenteEvent con estadoAnterior real y estadoNuevo AGENTE_ASIGNADO (Épica 2)")
    void publicaEventoConEstadoAnteriorReal() {
        Incidente incidente = incidenteConCAIyDenuncia();
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");

        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001")).thenReturn(List.of(agente));
        when(agenteRepo.intentarReservar("ag-001")).thenReturn(true);

        service.ejecutar("i-001");

        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        verify(eventPublisher).publicar(captor.capture());
        IncidenteEvent evento = captor.getValue();
        assertEquals("i-001", evento.getIncidenteId());
        assertEquals("d-001", evento.getDenuncianteId());
        assertEquals(com.callsos.backend.domain.enums.EstadoIncidente.DERIVADO_A_CAI, evento.getEstadoAnterior());
        assertEquals(com.callsos.backend.domain.enums.EstadoIncidente.AGENTE_ASIGNADO, evento.getEstadoNuevo());
    }

    @Test
    @DisplayName("Condición de carrera: si el primer candidato ya fue reservado, "
        + "reintenta con el siguiente en la lista")
    void reintentaConSiguienteCandidatoSiElPrimeroFueTomado() {
        Incidente incidente = incidenteConCAIyDenuncia();
        Agente primerCandidato  = new Agente("ag-001", "Pedro",  "Dir", ubicacion, "300");
        Agente segundoCandidato = new Agente("ag-002", "María",  "Dir", ubicacion, "301");

        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001"))
            .thenReturn(List.of(primerCandidato, segundoCandidato));

        // Simula que otra asignación concurrente ya tomó a "ag-001" justo
        // entre el SELECT (obtenerDisponiblesPorUnidad) y este punto.
        when(agenteRepo.intentarReservar("ag-001")).thenReturn(false);
        when(agenteRepo.intentarReservar("ag-002")).thenReturn(true);

        service.ejecutar("i-001");

        verify(agenteRepo).intentarReservar("ag-001");
        verify(agenteRepo).intentarReservar("ag-002");
        verify(asignacionRepo, times(1)).guardar(any());
    }

    @Test
    @DisplayName("Condición de carrera: si TODOS los candidatos ya fueron "
        + "reservados por otra asignación concurrente, lanza excepción sin persistir nada")
    void lanzaSiTodosLosCandidatosFueronTomadosPorConcurrencia() {
        Incidente incidente = incidenteConCAIyDenuncia();
        Agente candidato = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");

        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001")).thenReturn(List.of(candidato));
        when(agenteRepo.intentarReservar("ag-001")).thenReturn(false);

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));

        verify(asignacionRepo, never()).guardar(any());
        verify(incidenteRepo, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no existe")
    void lanzaSiIncidenteNoExiste() {
        when(incidenteRepo.buscarPorId("no-existe")).thenReturn(Optional.empty());
 
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe"));
 
        verifyNoInteractions(agenteRepo, asignacionRepo, eventPublisher);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no tiene CAI asignado")
    void lanzaSiSinCAI() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
 
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));
 
        verifyNoInteractions(agenteRepo, asignacionRepo, eventPublisher);
    }
 
    @Test
    @DisplayName("Lanza excepcion si no hay agentes disponibles en la unidad")
    void lanzaSiSinAgentesDisponibles() {
        Incidente incidente = incidenteConCAIyDenuncia();
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001")).thenReturn(List.of());
 
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));
 
        verify(asignacionRepo, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no tiene Denuncia vinculada")
    void lanzaSiSinDenuncia() {
        // Incidente con CAI pero sin denuncia
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI", "Dir", ubicacion, "600");
        incidente.derivarACAI(cai);
        // No se llama a setDenuncia

        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));

        // FIX (condición de carrera): el chequeo de Denuncia ahora ocurre
        // ANTES de tocar agenteRepository — precisamente para no reservar
        // (y dejar atascado en OCUPADO) un agente cuando el incidente de
        // todas formas iba a fallar por esta otra razón.
        verifyNoInteractions(agenteRepo, asignacionRepo, eventPublisher);
    }
 
    // ── Helper ─────────────────────────────────────────────────────────────
 
    private Incidente incidenteConCAIyDenuncia() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga", "Dir", ubicacion, "600");
        incidente.derivarACAI(cai);
        Denuncia denuncia = new Denuncia(
            "den-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante, incidente);
        incidente.setDenuncia(denuncia);
        return incidente;
    }
}