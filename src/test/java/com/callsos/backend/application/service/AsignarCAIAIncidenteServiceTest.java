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
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("AsignarCAIAIncidenteService")
class AsignarCAIAIncidenteServiceTest {
    
    @Mock IncidenteRepositoryPort incidenteRepo;
    @Mock UnidadPolicialRepositoryPort unidadRepo;
    @Mock EventPublisherPort eventPublisher;
 
    private AsignarCAIAIncidenteService service;
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "d-001", "Juan", "Cartagena", "300", "j@test.com");
 
    @BeforeEach
    void setUp() {
        service = new AsignarCAIAIncidenteService(incidenteRepo, unidadRepo, eventPublisher);
    }
 
    @Test
    @DisplayName("Deriva al CAI más cercano y cambia estado a DERIVADO_A_CAI")
    void derivaAlCAIMasCercano() {
        Incidente incidente = new Incidente("i-001", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante);
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga",
            "Calle 10", ubicacion, "6010000");
 
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(unidadRepo.buscarPorUbicacion(ubicacion)).thenReturn(Optional.of(cai));
 
        service.ejecutar("i-001");
 
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, incidente.getEstado());
        assertEquals(cai, incidente.getUnidadPolicial());
        verify(incidenteRepo).guardar(incidente);
    }
 
    @Test
    @DisplayName("Publica IncidenteEvent con estadoAnterior CREADO y estadoNuevo DERIVADO_A_CAI (Épica 2)")
    void publicaEventoConEstadoAnteriorReal() {
        Incidente incidente = new Incidente("i-001", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante);
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga",
            "Calle 10", ubicacion, "6010000");
 
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(unidadRepo.buscarPorUbicacion(ubicacion)).thenReturn(Optional.of(cai));
 
        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        service.ejecutar("i-001");
        verify(eventPublisher).publicar(captor.capture());
 
        IncidenteEvent evento = captor.getValue();
        assertEquals("i-001", evento.getIncidenteId());
        assertEquals("d-001", evento.getDenuncianteId());
        assertEquals(EstadoIncidente.CREADO, evento.getEstadoAnterior());
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, evento.getEstadoNuevo());
    }
 
    @Test
    @DisplayName("Lanza excepción si no hay CAI disponible")
    void lanzaSiNoHayCAI() {
        Incidente incidente = new Incidente("i-001", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante);
        when(incidenteRepo.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(unidadRepo.buscarPorUbicacion(any())).thenReturn(Optional.empty());
 
        assertThrows(IllegalStateException.class, () -> service.ejecutar("i-001"));
        verify(incidenteRepo, never()).guardar(any());
        verifyNoInteractions(eventPublisher);
    }
 
    @Test
    @DisplayName("Lanza excepción si el incidente no existe")
    void lanzaSiIncidenteNoExiste() {
        when(incidenteRepo.buscarPorId("no-existe")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe"));
        verifyNoInteractions(eventPublisher);
    }
}
