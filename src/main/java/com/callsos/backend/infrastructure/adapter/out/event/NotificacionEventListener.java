/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.event;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
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
     * AUD-5: escucha el IncidenteEvent genérico y notifica según el
     * estadoNuevo. Antes de este fix, `IncidenteEvent` se importaba en
     * esta clase pero NUNCA se escuchaba (import sin uso) — el
     * comentario de ActualizacionIncidenteWebSocketListener afirma que
     * "FCM notifica proactivamente a CAI/Agente aunque no tengan la app
     * abierta", pero eso solo era cierto para el cambio de tipo
     * (onTipoActualizado) — los dos momentos más críticos del flujo
     * (una unidad recibe un incidente nuevo, un agente es asignado)
     * jamás disparaban push: CAI/Agente solo se enteraban si abrían la
     * app y refrescaban manualmente (las home views no hacen polling).
     *
     * AGENTE_EN_CAMINO y los estados terminales (FINALIZADO/CANCELADO)
     * NO pasan por acá — tienen su propio evento dedicado
     * (AgenteEnCaminoEvent / IncidenteFinalizadoEvent) y ya estaban
     * cubiertos.
     */
    @Async
    @EventListener
    public void onIncidenteEvent(IncidenteEvent event) {
        switch (event.getEstadoNuevo()) {
            case DERIVADO_A_CAI -> notificarUnidadPorDerivacion(event.getIncidenteId());
            case AGENTE_ASIGNADO -> notificarAgentePorAsignacion(event.getIncidenteId());
            case EN_ATENCION -> notificarDenuncianteEnAtencion(event);
            default -> { /* CREADO: nadie a quien avisar todavía. */ }
        }
    }

    private void notificarUnidadPorDerivacion(String incidenteId) {
        incidenteRepository.buscarPorId(incidenteId)
            .map(Incidente::getUnidadPolicial)
            .ifPresent(unidad -> notificacionPort.notificarUnidadPolicial(
                unidad, "Un nuevo incidente fue derivado a tu CAI."));
    }

    private void notificarAgentePorAsignacion(String incidenteId) {
        asignacionRepository.buscarPorIncidente(incidenteId)
            .map(asignacion -> asignacion.getAgente())
            .ifPresent(agente -> notificacionPort.notificarAgente(
                agente, "Se te ha asignado un nuevo incidente."));
    }

    private void notificarDenuncianteEnAtencion(IncidenteEvent event) {
        denuncianteRepository.buscarPorId(event.getDenuncianteId())
            .ifPresent(denunciante -> notificacionPort.notificarDenunciante(
                denunciante, "El agente ha llegado y está atendiendo tu caso."));
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
     *
     * AUD-5: si el incidente tenía un agente asignado y se CANCELA
     * (ej. el denunciante cancela mientras el agente ya iba en camino
     * o estaba atendiendo), ese agente nunca se enteraba — seguía
     * manejando hacia un incidente cancelado hasta que abriera la app
     * manualmente. Ahora también se le notifica.
     *
     * Se usa buscarUltimaPorIncidente() (no buscarPorIncidente(), que
     * filtra por ACTIVA) porque para cuando este listener corre,
     * AgenteLiberador ya marcó la asignación como FINALIZADA de forma
     * síncrona, antes de publicar el evento — buscarPorIncidente()
     * siempre devolvería vacío acá. Ver el Javadoc del puerto.
     *
     * Solo aplica a CANCELADO: si el estado terminal es FINALIZADO, el
     * propio agente fue quien lo provocó (EvaluarIncidenteService /
     * CrearReporteHallazgosService) — ya lo sabe, notificarlo sería
     * redundante.
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

        if (event.getEstadoNuevo() == EstadoIncidente.CANCELADO) {
            asignacionRepository.buscarUltimaPorIncidente(event.getIncidenteId())
                .map(asignacion -> asignacion.getAgente())
                .ifPresent(agente -> notificacionPort.notificarAgente(
                    agente, "El incidente al que estabas asignado fue cancelado."));
        }
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