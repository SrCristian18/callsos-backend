/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Cubre PublicarUbicacionAgenteService — quedó sin test cuando la lógica
 * de "persistir + publicar en el topic STOMP" se extrajo de
 * UbicacionAgenteController hacia este servicio (para poder reutilizarla
 * también desde SimularRecorridoAgenteService, pruebas piloto de
 * simulación GPS). Antes de este test, ningún caso cubría directamente
 * la construcción de UbicacionAgente ni el payload publicado en el topic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicarUbicacionAgenteService")
class PublicarUbicacionAgenteServiceTest {

    @Mock UbicacionAgenteRepositoryPort repositorio;
    @Mock SimpMessagingTemplate messagingTemplate;

    private PublicarUbicacionAgenteService service;

    @BeforeEach
    void setUp() {
        service = new PublicarUbicacionAgenteService(repositorio, messagingTemplate);
    }

    @Test
    @DisplayName("Persiste la ubicación con el agenteId e incidenteId recibidos")
    void persisteUbicacion() {
        Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

        service.publicar("ag-001", "i-001", ubicacion);

        ArgumentCaptor<UbicacionAgente> captor = ArgumentCaptor.forClass(UbicacionAgente.class);
        verify(repositorio).guardar(captor.capture());

        UbicacionAgente guardada = captor.getValue();
        assertEquals("ag-001", guardada.getAgenteId());
        assertEquals("i-001", guardada.getIncidenteId());
        assertEquals(10.4, guardada.getUbicacion().getLatitud());
        assertEquals(-75.5, guardada.getUbicacion().getLongitud());
    }

    @Test
    @DisplayName("Publica en /topic/agente/{agenteId}/ubicacion con lat/lon de la Ubicacion recibida (Épica 3)")
    void publicaEnElTopicoCorrecto() {
        Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

        service.publicar("ag-001", "i-001", ubicacion);

        ArgumentCaptor<PublicarUbicacionAgenteService.UbicacionPublicada> captor =
            ArgumentCaptor.forClass(PublicarUbicacionAgenteService.UbicacionPublicada.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"), captor.capture());

        assertEquals(10.4, captor.getValue().latitud());
        assertEquals(-75.5, captor.getValue().longitud());
        assertNotNull(captor.getValue().timestamp());
    }

    @Test
    @DisplayName("Persiste antes de publicar, usando el mismo timestamp en ambos pasos")
    void timestampConsistenteEntrePersistenciaYPublicacion() {
        Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

        service.publicar("ag-001", "i-001", ubicacion);

        ArgumentCaptor<UbicacionAgente> guardadaCaptor =
            ArgumentCaptor.forClass(UbicacionAgente.class);
        ArgumentCaptor<PublicarUbicacionAgenteService.UbicacionPublicada> publicadaCaptor =
            ArgumentCaptor.forClass(PublicarUbicacionAgenteService.UbicacionPublicada.class);

        verify(repositorio).guardar(guardadaCaptor.capture());
        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"), publicadaCaptor.capture());

        assertEquals(
            guardadaCaptor.getValue().getTimestamp().toString(),
            publicadaCaptor.getValue().timestamp());
    }

    @Test
    @DisplayName("Épica 3: el topic se nombra por agenteId, NO por incidenteId — "
        + "el mismo agente en incidentes distintos publica en el MISMO topic")
    void topicSeNombraPorAgenteIdNoPorIncidenteId() {
        service.publicar("ag-001", "i-777", new Ubicacion(10.4, -75.5));
        service.publicar("ag-001", "i-888", new Ubicacion(11.0, -76.0));

        // Dos incidentes distintos, mismo agente -> mismo topic. Antes de
        // la Épica 3 esto habría sido "/topic/incidente/i-777/ubicacion" y
        // "/topic/incidente/i-888/ubicacion" (dos topics distintos); ahora
        // el topic depende del AGENTE, no del incidente.
        verify(messagingTemplate, times(2)).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class));
    }

    @Test
    @DisplayName("Distintos agentes publican en topics distintos")
    void topicVariaSegunAgenteId() {
        service.publicar("ag-002", "i-777", new Ubicacion(10.4, -75.5));

        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-002/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class));
        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class));
    }
}