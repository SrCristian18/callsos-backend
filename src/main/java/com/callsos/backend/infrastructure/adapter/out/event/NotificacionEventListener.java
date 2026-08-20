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
import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
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
    private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
 
    public NotificacionEventListener(NotificacionPort notificacionPort,
                                     DenuncianteRepositoryPort denuncianteRepository,
                                     IncidenteRepositoryPort incidenteRepository,
                                     AsignacionRepositoryPort asignacionRepository) {
        this.notificacionPort       = notificacionPort;
        this.denuncianteRepository  = denuncianteRepository;
        this.incidenteRepository    = incidenteRepository;
        this.asignacionRepository   = asignacionRepository;
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

    /**
     * Épica 5 (requisito 2 del pedido): notifica por FCM al agente
     * asignado y al CAI dueño del incidente cuando el denunciante
     * actualiza el tipo. Antes de esta épica, FCM solo llegaba al
     * denunciante — CAI/Agente solo se enteraban si volvían a consultar
     * por REST.
     *
     * Ninguna de las dos notificaciones es crítica para el flujo: si el
     * incidente todavía no tiene agente asignado (ej. tipo cambiado justo
     * después de crear el incidente, antes de derivar a CAI), simplemente
     * no hay agente a quien notificar — no es un error.
     */
    @Async
    @EventListener
    public void onTipoActualizado(TipoIncidenteActualizadoEvent event) {
        String mensaje = "El tipo del incidente fue actualizado: "
            + event.getTipoAnterior() + " → " + event.getTipoNuevo();

        asignacionRepository.buscarPorIncidente(event.getIncidenteId())
            .map(asignacion -> asignacion.getAgente())
            .ifPresent(agente -> notificacionPort.notificarAgente(agente, mensaje));

        incidenteRepository.buscarPorId(event.getIncidenteId())
            .map(Incidente::getUnidadPolicial)
            .ifPresent(unidad -> notificacionPort.notificarUnidadPolicial(unidad, mensaje));
    }
}