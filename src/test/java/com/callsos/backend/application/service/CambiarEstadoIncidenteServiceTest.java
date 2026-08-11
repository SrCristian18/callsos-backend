/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
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
@DisplayName("CambiarEstadoIncidenteService")
class CambiarEstadoIncidenteServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;

    CambiarEstadoIncidenteService service;

    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @BeforeEach
    void setUp() {
        service = new CambiarEstadoIncidenteService(incidenteRepository);
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
    @DisplayName("cambiar a CANCELADO funciona desde cualquier estado activo")
    void cambiarACancelado() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.AGENTE_ASIGNADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        service.ejecutar("i-001", EstadoIncidente.CANCELADO);

        assertEquals(EstadoIncidente.CANCELADO, incidente.getEstado());
        verify(incidenteRepository).guardar(incidente);
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException y no guarda")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", EstadoIncidente.DERIVADO_A_CAI));
        verify(incidenteRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("transición inválida propaga la excepción del agregado y no guarda")
    void transicionInvalida() {
        Incidente incidente = incidenteEnEstado(EstadoIncidente.CREADO);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        // CREADO solo puede ir a DERIVADO_A_CAI (o CANCELADO) — no a EN_ATENCION
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001", EstadoIncidente.EN_ATENCION));
        verify(incidenteRepository, never()).guardar(any());
    }
}
