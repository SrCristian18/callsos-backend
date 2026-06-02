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
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
 
/**
 * Listener de auditoría: registra automáticamente cada transición de estado.
 *
 * Los casos de uso no necesitan llamar explícitamente a la auditoría —
 * el listener la captura vía evento de dominio.
 *
 * El actor (quién hizo el cambio) se extrae del SecurityContext,
 * que fue populado por JwtAuthFilter al procesar el token JWT.
 *
 * @Async: el registro no bloquea el hilo HTTP del request.
 */
public class AuditoriaEventListener {
    
     private final AuditoriaRepositoryPort auditoriaRepository;
 
    public AuditoriaEventListener(AuditoriaRepositoryPort auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }
 
    @Async
    @EventListener
    public void onCambioEstado(IncidenteEvent event) {
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
 
        auditoriaRepository.registrar(new AuditoriaIncidente(
            event.getIncidenteId(),
            null,
            event.getEstadoNuevo(),
            actorId,
            actorRol,
            "Evento: " + event.getClass().getSimpleName()
        ));
    }
}
