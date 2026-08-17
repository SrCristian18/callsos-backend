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
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.DenunciaRepositoryPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
 
/**
 * Test del caso de uso CrearIncidenteService.
 * Usa mocks solo para los puertos de salida (infraestructura).
 * Verifica la orquestación del caso de uso, no la BD.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CrearIncidenteService")
class CrearIncidenteServiceTest {
    
     @Mock IncidenteRepositoryPort incidenteRepo;
    @Mock DenuncianteRepositoryPort denuncianteRepo;
    @Mock DenunciaRepositoryPort denunciaRepo;
    @Mock EventPublisherPort eventPublisher;
 
    private CrearIncidenteService service;
 
    private final Denunciante denunciante = new Denunciante(
        "d-001", "Juan Pérez", "Cartagena", "300", "j@test.com");
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
 
    @BeforeEach
    void setUp() {
        service = new CrearIncidenteService(incidenteRepo, denuncianteRepo, denunciaRepo, eventPublisher);
    }
 
    @Test
    @DisplayName("Crea incidente y lo persiste cuando el denunciante existe")
    void creaYPersiste() {
        when(denuncianteRepo.buscarPorId("d-001"))
            .thenReturn(Optional.of(denunciante));
 
        Incidente resultado = service.ejecutar(
            "d-001", TipoIncidente.ROBOS_O_ASALTOS, "Robo en la calle", ubicacion);
 
        assertNotNull(resultado);
        assertNotNull(resultado.getId());
        assertEquals(TipoIncidente.ROBOS_O_ASALTOS, resultado.getTipo());
        assertEquals(denunciante, resultado.getDenunciante());
        // FIX: verificar que el servicio también persiste la Denuncia
        // y la vincula al incidente (requisito de AsignarAgenteService).
        verify(incidenteRepo, times(1)).guardar(any(Incidente.class));
        verify(denunciaRepo,  times(1)).guardar(any());
        assertNotNull(resultado.getDenuncia(),
            "El incidente debe tener Denuncia vinculada tras ser creado");
    }
 
    @Test
    @DisplayName("Publica IncidenteEvent(CREADO) con estadoAnterior null (Épica 2, fix P4)")
    void publicaEventoDeCreacion() {
        when(denuncianteRepo.buscarPorId("d-001"))
            .thenReturn(Optional.of(denunciante));
 
        Incidente resultado = service.ejecutar(
            "d-001", TipoIncidente.ROBOS_O_ASALTOS, "Robo en la calle", ubicacion);
 
        ArgumentCaptor<IncidenteEvent> captor = ArgumentCaptor.forClass(IncidenteEvent.class);
        verify(eventPublisher).publicar(captor.capture());
 
        IncidenteEvent evento = captor.getValue();
        assertEquals(resultado.getId(), evento.getIncidenteId());
        assertEquals("d-001", evento.getDenuncianteId());
        assertNull(evento.getEstadoAnterior());
        assertEquals(EstadoIncidente.CREADO, evento.getEstadoNuevo());
    }
 
    @Test
    @DisplayName("Lanza excepción si el denunciante no existe")
    void lanzaSiDenuncianteNoExiste() {
        when(denuncianteRepo.buscarPorId("no-existe"))
            .thenReturn(Optional.empty());
 
        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar(
                "no-existe", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion));
 
        verify(incidenteRepo, never()).guardar(any());
        verify(denunciaRepo,  never()).guardar(any());
        verifyNoInteractions(eventPublisher);
    }
}