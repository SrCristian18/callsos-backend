/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteAdministrativo;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteAdministrativoRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
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
@DisplayName("CrearReporteAdministrativoService")
class CrearReporteAdministrativoServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;
    @Mock UnidadPolicialRepositoryPort unidadPolicialRepository;
    @Mock ReporteAdministrativoRepositoryPort reporteRepository;

    CrearReporteAdministrativoService service;

    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @BeforeEach
    void setUp() {
        service = new CrearReporteAdministrativoService(
            incidenteRepository, unidadPolicialRepository, reporteRepository);
    }

    @Test
    @DisplayName("crea y guarda el reporte con incidente y autoridad correctos")
    void creaReporteCorrectamente() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        UnidadPolicial cai = new UnidadPolicial(
            "cai-001", "CAI Manga", "Calle 1", new Ubicacion(10.4, -75.5), "6010000");

        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(unidadPolicialRepository.buscarPorId("cai-001")).thenReturn(Optional.of(cai));

        ReporteAdministrativo reporte = service.ejecutar("i-001", "cai-001", "Resumen del caso");

        assertNotNull(reporte.getId());
        assertEquals("Resumen del caso", reporte.getResumen());
        assertEquals(incidente, reporte.getIncidente());
        assertEquals(cai, reporte.getAutoridad());
        verify(reporteRepository).guardar(reporte);
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException y no guarda")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "cai-001", "resumen"));

        verifyNoInteractions(unidadPolicialRepository, reporteRepository);
    }

    @Test
    @DisplayName("autoridad inexistente lanza IllegalArgumentException y no guarda")
    void autoridadNoEncontrada() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(unidadPolicialRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("i-001", "no-existe", "resumen"));

        verify(reporteRepository, never()).guardar(any());
    }
}
