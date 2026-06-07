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
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 
    private AsignarAgenteService service;
 
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "d-001", "Juan", "Cartagena", "300", "j@test.com");
 
    @BeforeEach
    void setUp() {
        service = new AsignarAgenteService(agenteRepo, incidenteRepo, asignacionRepo);
    }
 
    @Test
    @DisplayName("Asigna agente disponible y persiste asignacion, incidente y estado agente")
    void asignaAgenteYPersisteTodo() {
        Incidente incidente = incidenteConCAIyDenuncia();
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");
 
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001")).thenReturn(List.of(agente));
 
        service.ejecutar("i-001");
 
        // Verifica que los 3 objetos afectados se persisten
        verify(asignacionRepo, times(1)).guardar(any());
        verify(incidenteRepo, times(1)).guardar(incidente);
        verify(agenteRepo, times(1)).actualizarEstado(agente);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no existe")
    void lanzaSiIncidenteNoExiste() {
        when(incidenteRepo.buscarPorId("no-existe")).thenReturn(Optional.empty());
 
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe"));
 
        verifyNoInteractions(agenteRepo, asignacionRepo);
    }
 
    @Test
    @DisplayName("Lanza excepcion si el incidente no tiene CAI asignado")
    void lanzaSiSinCAI() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
 
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));
 
        verifyNoInteractions(agenteRepo, asignacionRepo);
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
 
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(agenteRepo.obtenerDisponiblesPorUnidad("cai-001")).thenReturn(List.of(agente));
 
        assertThrows(IllegalStateException.class,
            () -> service.ejecutar("i-001"));
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
