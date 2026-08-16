package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActualizarTipoIncidenteService — Épica 1")
class ActualizarTipoIncidenteServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;

    ActualizarTipoIncidenteService service;

    private final Denunciante denuncianteDueno = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    private final Denunciante otroDenunciante = new Denunciante(
        "den-999", "Otro Denunciante", "Cartagena", "3002222222", "otro@test.com");

    @BeforeEach
    void setUp() {
        service = new ActualizarTipoIncidenteService(incidenteRepository);
    }

    private Incidente incidenteEnEstado(EstadoIncidente estado) {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denuncianteDueno);
        incidente.reconstituirEstado(estado);
        return incidente;
    }

    @Test
    @DisplayName("el dueño cambia ROBOS_O_ASALTOS → RIÑAS_O_PELEAS en estado activo y se persiste")
    void cambioExitosoPorElDueno() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", "den-001", TipoIncidente.RIÑAS_O_PELEAS);

        assertEquals(TipoIncidente.RIÑAS_O_PELEAS, incidente.getTipo());
        verify(incidenteRepository).guardar(incidente);
    }

    @Test
    @DisplayName("permite el cambio en cualquier estado activo (AGENTE_EN_CAMINO)")
    void cambioExitosoConAgenteEnCamino() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_EN_CAMINO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", "den-001", TipoIncidente.RIÑAS_O_PELEAS);

        assertEquals(TipoIncidente.RIÑAS_O_PELEAS, incidente.getTipo());
        verify(incidenteRepository).guardar(incidente);
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException (404) y no guarda")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "den-001", TipoIncidente.RIÑAS_O_PELEAS));
        verify(incidenteRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("denunciante ajeno (no dueño) recibe AccesoDenegadoException (403) y no guarda")
    void denuncianteAjenoRechazado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(AccesoDenegadoException.class,
            () -> service.ejecutar("i-001", otroDenunciante.getId(), TipoIncidente.RIÑAS_O_PELEAS));

        assertEquals(TipoIncidente.ROBOS_O_ASALTOS, incidente.getTipo(),
            "El tipo no debe cambiar si el actor no es el dueño");
        verify(incidenteRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("incidente FINALIZADO rechaza el cambio con IllegalStateException (422) y no guarda")
    void incidenteFinalizadoRechazado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.FINALIZADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001", "den-001", TipoIncidente.RIÑAS_O_PELEAS));
        verify(incidenteRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("incidente CANCELADO rechaza el cambio con IllegalStateException (422) y no guarda")
    void incidenteCanceladoRechazado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CANCELADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001", "den-001", TipoIncidente.RIÑAS_O_PELEAS));
        verify(incidenteRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("mismo tipo rechaza el cambio con IllegalStateException (422) y no guarda")
    void mismoTipoRechazado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001", "den-001", TipoIncidente.ROBOS_O_ASALTOS));
        verify(incidenteRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("la validación de ownership ocurre antes que la de estado (orden defensivo)")
    void ownershipSeValidaAntesQueEstado() {
        // Incidente ajeno Y finalizado: debe fallar por ownership (403),
        // no por estado (422) — evita filtrar información de estado
        // de un incidente que no le pertenece al actor.
        Incidente incidente = incidenteEnEstado(EstadoIncidente.FINALIZADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(AccesoDenegadoException.class,
            () -> service.ejecutar("i-001", otroDenunciante.getId(), TipoIncidente.RIÑAS_O_PELEAS));
        verify(incidenteRepository, never()).guardar(any());
    }
}
