/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.application.service.support.AgenteLiberador;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AgenteByIdRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("CrearReporteHallazgosService")
public class CrearReporteHallazgosServiceTest {
    
    @Mock IncidenteRepositoryPort incidenteRepo;
    @Mock AgenteByIdRepositoryPort agenteRepo;
    @Mock ReporteHallazgosRepositoryPort reporteRepo;
    @Mock AgenteLiberador agenteLiberador;
 
    private CrearReporteHallazgosService service;
 
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "d-001", "Juan", "Cartagena", "300", "j@test.com");
 
    @BeforeEach
    void setUp() {
        service = new CrearReporteHallazgosService(incidenteRepo, agenteRepo, reporteRepo, agenteLiberador);
    }
 
    @Test
    @DisplayName("Crea reporte y finaliza el incidente")
    void creaReporteYFinalizaIncidente() {
        Incidente incidente = incidenteEnAtencion();
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");
 
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.buscarPorId("ag-001")).thenReturn(Optional.of(agente));
 
        ReporteHallazgos reporte = service.ejecutar("i-001", "ag-001", "Situación controlada");
 
        assertNotNull(reporte);
        assertNotNull(reporte.getId());
        assertEquals("Situación controlada", reporte.getDescripcion());
        assertEquals(EstadoIncidente.FINALIZADO, incidente.getEstado());
 
        verify(reporteRepo).guardar(any(ReporteHallazgos.class));
        verify(incidenteRepo).guardar(incidente);
    }

    @Test
    @DisplayName("FIX agente OCUPADO para siempre: ESTE es el flujo real que usa la app "
        + "(ReporteHallazgosView -> POST /reportes/hallazgos), libera al agente tras finalizar")
    void liberaAlAgenteAsignado() {
        Incidente incidente = incidenteEnAtencion();
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");

        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.buscarPorId("ag-001")).thenReturn(Optional.of(agente));

        service.ejecutar("i-001", "ag-001", "Situación controlada");

        var inOrder = inOrder(incidenteRepo, agenteLiberador);
        inOrder.verify(incidenteRepo).guardar(incidente);
        inOrder.verify(agenteLiberador).liberarSiHayAsignacionActiva("i-001");
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no esta EN_ATENCION")
    void lanzaSiEstadoIncorrecto() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        // Estado: CREADO, no EN_ATENCION
 
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.buscarPorId("ag-001"))
            .thenReturn(Optional.of(new Agente("ag-001", "Pedro", "Dir", ubicacion, "300")));
 
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001", "ag-001", "descripcion"));
 
        verify(reporteRepo, never()).guardar(any());
        verify(incidenteRepo, never()).guardar(any());
        verifyNoInteractions(agenteLiberador);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no existe")
    void lanzaSiIncidenteNoExiste() {
        when(incidenteRepo.buscarPorId("no-existe")).thenReturn(Optional.empty());
 
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "ag-001", "desc"));
 
        verifyNoInteractions(agenteRepo, reporteRepo, agenteLiberador);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el agente no existe")
    void lanzaSiAgenteNoExiste() {
        Incidente incidente = incidenteEnAtencion();
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.buscarPorId("no-existe")).thenReturn(Optional.empty());
 
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("i-001", "no-existe", "desc"));
 
        verify(reporteRepo, never()).guardar(any());
        verifyNoInteractions(agenteLiberador);
    }
 
    // ── Helper ─────────────────────────────────────────────────────────────
 
    private Incidente incidenteEnAtencion() {
        Incidente i = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        i.derivarACAI(new UnidadPolicial("cai-001", "CAI", "Dir", ubicacion, "600"));
        i.marcarAgenteAsignado();
        i.marcarAgenteEnCamino();
        i.iniciarAtencion();
        return i;
    }
}