package com.callsos.backend.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Configuración de beans de prueba.
 *
 * @TestConfiguration garantiza que estos beans solo existen en el
 * classpath de test — nunca en producción.
 *
 * FIX (Épica 5 — "error menor en testconfig"): antes existía aquí un
 * bean stub de NotificacionPort:
 *
 *   @Bean
 *   @ConditionalOnMissingBean(NotificacionPort.class)
 *   public NotificacionPort notificacionPortStub() {
 *       return (denunciante, mensaje) -> { };  // lambda de 1 solo método
 *   }
 *
 * La Épica 5 amplió NotificacionPort de 1 a 3 métodos abstractos
 * (agregó notificarAgente y notificarUnidadPolicial — ver
 * ActualizacionIncidenteWebSocketListener / NotificacionEventListener).
 * Una lambda solo puede implementar una interfaz funcional (1 método
 * abstracto), así que ese stub dejó de compilar — rompía TODO el
 * módulo de test, no solo los tests que lo usaban.
 *
 * En vez de convertir la lambda en una clase anónima con los 3 métodos
 * (duplicando lógica), se eliminó por completo: NotificationConfig
 * (configuración real, siempre activa — no solo en test) YA registra
 * NotificacionNoOpAdapter bajo la MISMA condición
 * (@ConditionalOnMissingBean(NotificacionPort.class), ver
 * NotificationConfig.noOpNotification()), y NotificacionNoOpAdapter
 * SÍ implementa correctamente los 3 métodos. Este stub era redundante
 * incluso antes de la Épica 5: los dos únicos tests que lo importaban
 * (BackendApplicationTests, CrearIncidenteAsignarAgenteFlujoIntegracionTest)
 * usan @SpringBootTest — cargan el contexto completo, así que
 * NotificationConfig ya se registra de todas formas con o sin este
 * @Import(TestConfig.class). Mantener dos implementaciones no-op del
 * mismo puerto (una en producción, otra en test) es exactamente el
 * tipo de duplicación que causó este bug: cuando el puerto creció, se
 * actualizó una y se olvidó la otra.
 *
 * La clase se conserva vacía (en vez de eliminar el archivo y sus 2
 * imports) como punto de extensión para futuros beans que sí sean
 * legítimamente exclusivos de test.
 */
@TestConfiguration
public class TestConfig {
}