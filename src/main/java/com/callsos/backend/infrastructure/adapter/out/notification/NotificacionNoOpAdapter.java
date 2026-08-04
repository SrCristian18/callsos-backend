package com.callsos.backend.infrastructure.adapter.out.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.callsos.backend.domain.model.Denunciante;
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
    
}
