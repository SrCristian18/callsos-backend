package com.callsos.backend.infrastructure.config;

import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.out.NotificacionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Configuración de beans de prueba.
 *
 * Proporciona implementaciones stub de beans que no están disponibles
 * en el entorno de test (Firebase deshabilitado con firebase.enabled=false).
 *
 * @TestConfiguration garantiza que estos beans solo existen en el
 * classpath de test — nunca en producción.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Stub de NotificacionPort para tests.
     *
     * Cuando firebase.enabled=false, NotificacionFirebaseAdapter no se
     * registra (@ConditionalOnProperty) y Spring no tiene implementación
     * de NotificacionPort. Este bean stub llena ese hueco en tests:
     * no hace nada al notificar (comportamiento correcto para tests que
     * no verifican FCM).
     *
     * @ConditionalOnMissingBean: si por alguna razón el adaptador real
     * estuviera presente, este stub no se registra — evita conflicto.
     */
    @Bean
    @ConditionalOnMissingBean(NotificacionPort.class)
    public NotificacionPort notificacionPortStub() {
        return (denunciante, mensaje) -> {
            // No-op stub: en tests no se envían notificaciones push reales.
        };
    }
}