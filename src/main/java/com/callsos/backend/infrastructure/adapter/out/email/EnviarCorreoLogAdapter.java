/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.email;

import com.callsos.backend.domain.port.out.EnviarCorreoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: implementación "log" de EnviarCorreoPort.
 *
 * Épica 8 (hallazgo #6, Parte 2): NO envía correo real — el proyecto no
 * tiene todavía una cuenta/proveedor SMTP configurado (decisión
 * explícita, no descuido: ver EnviarCorreoPort). Deja el contenido
 * completo del correo en los logs del servidor, suficiente para probar
 * el flujo de recuperación de contraseña de punta a punta en desarrollo
 * sin bloquear el resto de la implementación.
 *
 * IMPORTANTE — por qué esto es más seguro que devolver el token en la
 * respuesta HTTP: los logs del servidor son un canal restringido
 * (acceso solo de infraestructura/devs), mientras que la respuesta HTTP
 * es visible para cualquiera que intercepte esa conexión concreta. Un
 * flujo de "recuperar contraseña" que expone el token de reseteo en su
 * propia respuesta rompe la garantía que se supone debe dar (que solo
 * quien tiene acceso al correo puede resetear la contraseña).
 *
 * Para producción: crear un nuevo @Component que implemente
 * EnviarCorreoPort usando un proveedor real (spring-boot-starter-mail +
 * SMTP, SendGrid, Mailgun, etc.) y reemplazar el bean en
 * ApplicationConfig. Cero cambios en SolicitarReseteoPasswordService.
 */
@Component
public class EnviarCorreoLogAdapter implements EnviarCorreoPort {

    private static final Logger log = LoggerFactory.getLogger(EnviarCorreoLogAdapter.class);

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.info("""
            ══════════════════════════════════════════════════════════════
            [EMAIL] Adaptador LOG (Épica 8, hallazgo #6 — sin proveedor SMTP real configurado)
            Para:    {}
            Asunto:  {}
            ──────────────────────────────────────────────────────────────
            {}
            ══════════════════════════════════════════════════════════════
            """, destinatario, asunto, cuerpo);
    }
}