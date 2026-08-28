package com.callsos.backend.infrastructure.adapter.out.event;

import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Épica 5 — propaga en tiempo real los cambios de tipo de incidente a
 * quienes ya tienen (o podrían tener) el incidente abierto en pantalla:
 * CAI, Agente asignado y Comando.
 *
 * Publica a /topic/incidente/{id}/actualizaciones — el mismo topic
 * "informativo por incidente" ya anticipado en el comentario de
 * PublicarUbicacionAgenteService.publicarEtaSiCorresponde() (Épica 4):
 * no requiere autorización de SUBSCRIBE en StompAuthChannelInterceptor,
 * a diferencia de /topic/agente/{agenteId}/ubicacion (Épica 3, fix P6).
 * La razón es la misma que para el ETA: el tipo de un incidente no es
 * información sensible al nivel del GPS del agente — quien ya conoce el
 * incidenteId (porque el incidente le fue derivado/asignado, o es
 * Comando) puede verlo también por REST (GET /incidentes/{id}), este
 * topic solo evita que tenga que hacer polling.
 *
 * Separado de NotificacionEventListener (FCM) a propósito: son dos
 * canales de entrega distintos con audiencias parcialmente distintas
 * (FCM notifica proactivamente a CAI/Agente aunque no tengan la app
 * abierta; este topic actualiza en vivo a quien SÍ la tiene abierta,
 * incluido Comando, que FCM no cubre) — mezclar ambas responsabilidades
 * en una sola clase haría más difícil, por ejemplo, cambiar de proveedor
 * de push sin tocar la propagación WebSocket, o viceversa.
 */
@Component
public class ActualizacionIncidenteWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    public ActualizacionIncidenteWebSocketListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @EventListener
    public void onTipoActualizado(TipoIncidenteActualizadoEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/incidente/" + event.getIncidenteId() + "/actualizaciones",
            new ActualizacionPublicada(
                "TIPO_ACTUALIZADO",
                event.getTipoAnterior() != null ? event.getTipoAnterior().name() : null,
                event.getTipoNuevo().name(),
                event.getOcurridoEn().toString()
            )
        );
    }

    /**
     * Payload genérico deliberadamente — "tipoEvento" + valor
     * anterior/nuevo permite que, si en el futuro se agregan otras
     * actualizaciones a este mismo topic (ej. reasignación de CAI), el
     * cliente Flutter no necesite un modelo nuevo por cada una, solo
     * ramificar por tipoEvento.
     */
    public record ActualizacionPublicada(
        String tipoEvento,
        String valorAnterior,
        String valorNuevo,
        String timestamp
    ) {}
}
