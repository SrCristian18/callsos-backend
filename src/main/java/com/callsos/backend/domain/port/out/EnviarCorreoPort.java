/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 * Puerto de salida: envío de correo electrónico.
 *
 * Épica 8 (hallazgo #6, Parte 2). Se desacopla detrás de un puerto —
 * mismo criterio arquitectónico que el resto del proyecto (ver
 * EventPublisherPort para notificaciones WebSocket/push) — para que el
 * mecanismo real de envío (SMTP, SendGrid, Mailgun, lo que se decida)
 * sea un detalle de infraestructura intercambiable sin tocar dominio ni
 * casos de uso.
 *
 * IMPLEMENTACIÓN ACTUAL: EnviarCorreoLogAdapter — NO envía correo real,
 * solo lo deja en los logs del servidor. Es una decisión explícita
 * (Épica 8): el proyecto no tiene todavía una cuenta/proveedor SMTP
 * real configurado. Cuando se decida uno, se agrega un nuevo
 * @Component que implemente este mismo puerto (ej.
 * EnviarCorreoSmtpAdapter usando spring-boot-starter-mail) y se
 * reemplaza el bean en ApplicationConfig — cero cambios en
 * SolicitarReseteoPasswordService ni en ningún otro consumidor.
 */
public interface EnviarCorreoPort {

    /**
     * @param destinatario  Correo del destinatario
     * @param asunto        Asunto del correo
     * @param cuerpo        Cuerpo en texto plano
     */
    void enviar(String destinatario, String asunto, String cuerpo);
}