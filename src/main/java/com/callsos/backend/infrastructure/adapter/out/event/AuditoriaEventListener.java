/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.event;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
 
/**
 * Listener de auditoría: registra automáticamente cada hecho auditable
 * del incidente — transiciones de estado y (Épica 2) cambios de campo
 * genérico como la actualización del tipo.
 *
 * Los casos de uso no necesitan llamar explícitamente a la auditoría —
 * el listener la captura vía evento de dominio.
 *
 * El actor (quién hizo el cambio) se extrae del SecurityContext,
 * que fue populado por JwtAuthFilter al procesar el token JWT.
 *
 * @Async: el registro no bloquea el hilo HTTP del request.
 *
 * Épica 2 (fix P4): antes solo AgenteEnCaminoEvent e IncidenteFinalizadoEvent
 * llegaban acá, porque eran los únicos eventos publicados en todo el
 * ciclo de vida. Ahora CrearIncidenteService, AsignarCAIAIncidenteService,
 * AsignarAgenteService, AtenderIncidenteService y CambiarEstadoIncidenteService
 * también publican IncidenteEvent, así que este mismo listener cubre las
 * 7 transiciones (CREADO, DERIVADO_A_CAI, AGENTE_ASIGNADO, AGENTE_EN_CAMINO,
 * EN_ATENCION, FINALIZADO, CANCELADO) sin necesidad de un listener por caso
 * de uso.
 *
 * Fix (Épica 2): esta clase NUNCA tuvo @Component — nunca fue un bean de
 * Spring y por lo tanto @EventListener jamás se registraba. En producción
 * la auditoría no se escribía en absoluto, independientemente de cuántos
 * casos de uso publicaran eventos. Se detectó al revisar la config antes
 * de ampliar esta clase.
 */
@Component
public class AuditoriaEventListener {
    
     private final AuditoriaRepositoryPort auditoriaRepository;
 
    public AuditoriaEventListener(AuditoriaRepositoryPort auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }
 
    /**
     * Registra transiciones de estado. TipoIncidenteActualizadoEvent
     * también es un IncidenteEvent (por el contrato de EventPublisherPort),
     * así que Spring dispararía este método igual que onCambioTipo() para
     * ese evento — se excluye explícitamente para no duplicar el registro
     * de auditoría con una fila que además diría "transición" a un estado
     * que en realidad no cambió.
     */
    @Async
    @EventListener
    public void onCambioEstado(IncidenteEvent event) {
        if (event instanceof TipoIncidenteActualizadoEvent) return;

        Actor actor = resolverActor();

        auditoriaRepository.registrar(new AuditoriaIncidente(
            event.getIncidenteId(),
            event.getEstadoAnterior(),
            event.getEstadoNuevo(),
            actor.id(),
            actor.rol(),
            "Evento: " + event.getClass().getSimpleName()
        ));
    }

    /** Registra el cambio de tipo como un hecho genérico, no como transición de estado. */
    @Async
    @EventListener
    public void onCambioTipo(TipoIncidenteActualizadoEvent event) {
        Actor actor = resolverActor();

        auditoriaRepository.registrar(AuditoriaIncidente.deCambioGenerico(
            event.getIncidenteId(),
            event.getEstadoNuevo(),
            actor.id(),
            actor.rol(),
            "Tipo actualizado: " + event.getTipoAnterior() + " → " + event.getTipoNuevo(),
            "tipo",
            event.getTipoAnterior() != null ? event.getTipoAnterior().name() : null,
            event.getTipoNuevo().name()
        ));
    }

    private Actor resolverActor() {
        String actorId  = "sistema";
        String actorRol = "SISTEMA";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            actorId = auth.getPrincipal().toString();
            actorRol = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DESCONOCIDO");
        }
        return new Actor(actorId, actorRol);
    }

    private record Actor(String id, String rol) {}
}
