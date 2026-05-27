/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.event;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.event.AgenteEnCaminoEvent;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.event.IncidenteFinalizadoEvent;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.NotificacionPort;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
 
/**
 * Listener de eventos de dominio para notificaciones push.
 *
 * DESACOPLAMIENTO CLAVE:
 *   Los casos de uso publican eventos y no saben nada de Firebase.
 *   Este listener vive en infraestructura y conecta el evento con FCM.
 *   Si mañana se reemplaza Firebase por otro proveedor, solo cambia este archivo.
 *
 * @Async: las notificaciones push se envían en un hilo separado para no
 *   bloquear el hilo HTTP del request. El usuario recibe el 204 inmediatamente
 *   y Firebase procesa en background.
 *   Requiere @EnableAsync en AsyncConfig.
 */
@Component
public class NotificacionEventListener {
    
    private final NotificacionPort notificacionPort;
    private final DenuncianteRepositoryPort denuncianteRepository;
 
    public NotificacionEventListener(NotificacionPort notificacionPort,
                                     DenuncianteRepositoryPort denuncianteRepository) {
        this.notificacionPort       = notificacionPort;
        this.denuncianteRepository  = denuncianteRepository;
    }
 
    /**
     * Escucha AgenteEnCaminoEvent:
     * Notifica al denunciante que el agente ya va en camino.
     */
    @Async
    @EventListener
    public void onAgenteEnCamino(AgenteEnCaminoEvent event) {
        denuncianteRepository.buscarPorId(event.getDenuncianteId())
            .ifPresent(denunciante ->
                notificacionPort.notificarDenunciante(
                    denunciante,
                    "Un agente de policía va en camino a tu ubicación."
                )
            );
    }
 
    /**
     * Escucha IncidenteFinalizadoEvent:
     * Notifica al denunciante según si fue finalizado o cancelado.
     */
    @Async
    @EventListener
    public void onIncidenteFinalizado(IncidenteFinalizadoEvent event) {
        denuncianteRepository.buscarPorId(event.getDenuncianteId())
            .ifPresent(denunciante -> {
                String mensaje = switch (event.getEstadoNuevo()) {
                    case FINALIZADO -> "Tu incidente ha sido atendido exitosamente.";
                    case CANCELADO  -> "Tu incidente ha sido cancelado.";
                    default         -> "El estado de tu incidente ha cambiado: "
                                       + event.getEstadoNuevo();
                };
                notificacionPort.notificarDenunciante(denunciante, mensaje);
            });
    }
}
