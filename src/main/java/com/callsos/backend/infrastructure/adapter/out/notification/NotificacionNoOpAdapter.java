package com.callsos.backend.infrastructure.adapter.out.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.NotificacionPort;

public class NotificacionNoOpAdapter implements NotificacionPort{

    private static final Logger log =
        LoggerFactory.getLogger(NotificacionNoOpAdapter.class);

    @Override
    public void notificarDenunciante(Denunciante denunciante, String mensaje) {
        log.info(
            "Firebase no configurado. No se envió notificacion para {} -> {}",
            denunciante.getNombre(),
            mensaje
        );
    }

    /** Épica 5 — mismo no-op que notificarDenunciante(), para el agente. */
    @Override
    public void notificarAgente(Agente agente, String mensaje) {
        log.info(
            "Firebase no configurado. No se envió notificacion para agente {} -> {}",
            agente.getNombre(),
            mensaje
        );
    }

    /** Épica 5 — mismo no-op que notificarDenunciante(), para el CAI. */
    @Override
    public void notificarUnidadPolicial(UnidadPolicial unidad, String mensaje) {
        log.info(
            "Firebase no configurado. No se envió notificacion para unidad {} -> {}",
            unidad.getNombre(),
            mensaje
        );
    }

}
