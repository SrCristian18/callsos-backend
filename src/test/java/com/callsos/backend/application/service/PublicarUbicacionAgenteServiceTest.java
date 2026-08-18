/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.CategoriaDistancia;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
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

import java.util.Optional;

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
 *
 * Épica 4: cada llamada a publicar() también dispara (o no, según
 * corresponda) el broadcast de ETA a /topic/incidente/{id}/eta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicarUbicacionAgenteService")
class PublicarUbicacionAgenteServiceTest {

    @Mock
    UbicacionAgenteRepositoryPort repositorio;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    IncidenteRepositoryPort incidenteRepository;

    private PublicarUbicacionAgenteService service;

    private static final double VELOCIDAD_MEDIA_KMH = 36.0; // 10 m/s, redondea fácil

    private final Denunciante denunciante = new Denunciante(
        "den-001",
        "Juan",
        "Cartagena",
        "300",
        "j@test.com"
    );

    @BeforeEach
    void setUp() {
        service = new PublicarUbicacionAgenteService(
            repositorio,
            messagingTemplate,
            incidenteRepository,
            VELOCIDAD_MEDIA_KMH
        );
    }

    private Incidente incidenteEnCamino(String id, Ubicacion destino) {
        Incidente incidente = new Incidente(
            id,
            TipoIncidente.ROBOS_O_ASALTOS,
            "desc",
            destino,
            denunciante
        );

        incidente.reconstituirEstado(EstadoIncidente.AGENTE_EN_CAMINO);

        return incidente;
    }

    @Test
    @DisplayName("Persiste la ubicación con el agenteId e incidenteId recibidos")
    void persisteUbicacion() {
        Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

        when(incidenteRepository.buscarPorId("i-001"))
            .thenReturn(Optional.empty());

        service.publicar(
            "ag-001",
            "i-001",
            ubicacion
        );

        ArgumentCaptor<UbicacionAgente> captor =
            ArgumentCaptor.forClass(UbicacionAgente.class);

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

        when(incidenteRepository.buscarPorId("i-001"))
            .thenReturn(Optional.empty());

        service.publicar(
            "ag-001",
            "i-001",
            ubicacion
        );

        ArgumentCaptor<PublicarUbicacionAgenteService.UbicacionPublicada> captor =
            ArgumentCaptor.forClass(
                PublicarUbicacionAgenteService.UbicacionPublicada.class
            );

        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            captor.capture()
        );

        assertEquals(10.4, captor.getValue().latitud());
        assertEquals(-75.5, captor.getValue().longitud());
        assertNotNull(captor.getValue().timestamp());
    }

    @Test
    @DisplayName("Persiste antes de publicar, usando el mismo timestamp en ambos pasos")
    void timestampConsistenteEntrePersistenciaYPublicacion() {
        Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

        when(incidenteRepository.buscarPorId("i-001"))
            .thenReturn(Optional.empty());

        service.publicar(
            "ag-001",
            "i-001",
            ubicacion
        );

        ArgumentCaptor<UbicacionAgente> guardadaCaptor =
            ArgumentCaptor.forClass(UbicacionAgente.class);

        ArgumentCaptor<PublicarUbicacionAgenteService.UbicacionPublicada> publicadaCaptor =
            ArgumentCaptor.forClass(
                PublicarUbicacionAgenteService.UbicacionPublicada.class
            );

        verify(repositorio).guardar(guardadaCaptor.capture());

        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            publicadaCaptor.capture()
        );

        assertEquals(
            guardadaCaptor.getValue().getTimestamp().toString(),
            publicadaCaptor.getValue().timestamp()
        );
    }

    @Test
    @DisplayName(
        "Épica 3: el topic se nombra por agenteId, NO por incidenteId — "
        + "el mismo agente en incidentes distintos publica en el MISMO topic"
    )
    void topicSeNombraPorAgenteIdNoPorIncidenteId() {
        when(incidenteRepository.buscarPorId(any()))
            .thenReturn(Optional.empty());

        service.publicar(
            "ag-001",
            "i-777",
            new Ubicacion(10.4, -75.5)
        );

        service.publicar(
            "ag-001",
            "i-888",
            new Ubicacion(11.0, -76.0)
        );

        // Dos incidentes distintos, mismo agente -> mismo topic.
        // Antes de la Épica 3 esto habría sido:
        // "/topic/incidente/i-777/ubicacion"
        // "/topic/incidente/i-888/ubicacion"
        //
        // Ahora el topic depende del AGENTE, no del incidente.
        verify(messagingTemplate, times(2)).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class)
        );
    }

    @Test
    @DisplayName("Distintos agentes publican en topics distintos")
    void topicVariaSegunAgenteId() {
        when(incidenteRepository.buscarPorId("i-777"))
            .thenReturn(Optional.empty());

        service.publicar(
            "ag-002",
            "i-777",
            new Ubicacion(10.4, -75.5)
        );

        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-002/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class)
        );

        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class)
        );
    }

    // ── Épica 4: broadcast de ETA ────────────────────────────────────────

    @Test
    @DisplayName(
        "incidente AGENTE_EN_CAMINO: también publica ETA en "
        + "/topic/incidente/{id}/eta, sin lat/lon"
    )
    void publicaEtaCuandoIncidenteEstaEnCamino() {

        // Destino a ~550m al norte del origen reportado (aprox.)
        // — bien por debajo del límite de 1km para no depender
        // de precisión de trigonometría cerca del borde de la categoría.
        Ubicacion destino = new Ubicacion(10.405, -75.5);

        Incidente incidente =
            incidenteEnCamino("i-001", destino);

        when(incidenteRepository.buscarPorId("i-001"))
            .thenReturn(Optional.of(incidente));

        service.publicar(
            "ag-001",
            "i-001",
            new Ubicacion(10.4, -75.5)
        );

        ArgumentCaptor<PublicarUbicacionAgenteService.EtaPublicada> captor =
            ArgumentCaptor.forClass(
                PublicarUbicacionAgenteService.EtaPublicada.class
            );

        verify(messagingTemplate).convertAndSend(
            eq("/topic/incidente/i-001/eta"),
            captor.capture()
        );

        assertNotNull(captor.getValue().minutosEstimados());

        assertTrue(
            captor.getValue().minutosEstimados() > 0
        );

        assertEquals(
            CategoriaDistancia.MENOS_DE_1_KM,
            captor.getValue().categoriaDistancia()
        );
    }

    @Test
    @DisplayName("incidente en otro estado (ej. EN_ATENCION): NO publica ETA")
    void noPublicaEtaSiNoEstaEnCamino() {

        Incidente incidente = new Incidente(
            "i-002",
            TipoIncidente.ROBOS_O_ASALTOS,
            "desc",
            new Ubicacion(10.409, -75.5),
            denunciante
        );

        incidente.reconstituirEstado(
            EstadoIncidente.EN_ATENCION
        );

        when(incidenteRepository.buscarPorId("i-002"))
            .thenReturn(Optional.of(incidente));

        service.publicar(
            "ag-001",
            "i-002",
            new Ubicacion(10.4, -75.5)
        );

        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/incidente/i-002/eta"),
            any(PublicarUbicacionAgenteService.EtaPublicada.class)
        );
    }

    @Test
    @DisplayName("incidente inexistente: no falla, simplemente no publica ETA")
    void noPublicaEtaSiIncidenteNoExiste() {

        when(incidenteRepository.buscarPorId("no-existe"))
            .thenReturn(Optional.empty());

        service.publicar(
            "ag-001",
            "no-existe",
            new Ubicacion(10.4, -75.5)
        );

        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/incidente/no-existe/eta"),
            any(PublicarUbicacionAgenteService.EtaPublicada.class)
        );

        // La ubicación sí se sigue publicando con normalidad.
        verify(messagingTemplate).convertAndSend(
            eq("/topic/agente/ag-001/ubicacion"),
            any(PublicarUbicacionAgenteService.UbicacionPublicada.class)
        );
    }
}