package com.callsos.backend.infrastructure.adapter.out.event;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Épica 5 — cubre el nuevo listener que propaga el cambio de tipo de
 * incidente a /topic/incidente/{id}/actualizaciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActualizacionIncidenteWebSocketListener")
class ActualizacionIncidenteWebSocketListenerTest {

    @Mock SimpMessagingTemplate messagingTemplate;

    ActualizacionIncidenteWebSocketListener listener;

    @BeforeEach
    void setUp() {
        listener = new ActualizacionIncidenteWebSocketListener(messagingTemplate);
    }

    @Test
    @DisplayName("publica en /topic/incidente/{id}/actualizaciones con tipo anterior y nuevo")
    void publicaActualizacionDeTipo() {
        TipoIncidenteActualizadoEvent event = new TipoIncidenteActualizadoEvent(
            "i-001", "den-001", EstadoIncidente.DERIVADO_A_CAI,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS);

        listener.onTipoActualizado(event);

        ArgumentCaptor<ActualizacionIncidenteWebSocketListener.ActualizacionPublicada> captor =
            ArgumentCaptor.forClass(ActualizacionIncidenteWebSocketListener.ActualizacionPublicada.class);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/incidente/i-001/actualizaciones"), captor.capture());

        ActualizacionIncidenteWebSocketListener.ActualizacionPublicada publicada = captor.getValue();
        assertEquals("TIPO_ACTUALIZADO", publicada.tipoEvento());
        assertEquals("ROBOS_O_ASALTOS", publicada.valorAnterior());
        assertEquals("RIÑAS_O_PELEAS", publicada.valorNuevo());
        assertNotNull(publicada.timestamp());
    }

    @Test
    @DisplayName("distintos incidentes publican en topics distintos")
    void topicVariaSegunIncidenteId() {
        TipoIncidenteActualizadoEvent event = new TipoIncidenteActualizadoEvent(
            "i-777", "den-001", EstadoIncidente.CREADO,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS);

        listener.onTipoActualizado(event);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/incidente/i-777/actualizaciones"),
            org.mockito.ArgumentMatchers.any(
                ActualizacionIncidenteWebSocketListener.ActualizacionPublicada.class));
    }
}
