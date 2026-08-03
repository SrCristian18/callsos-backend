/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.notification;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.out.NotificacionPort;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

/**
 * Adaptador de salida: envía notificaciones push vía Firebase Cloud Messaging.
 *
 * FCM es gratuito y sin límite de mensajes (verificado en Fase 2).
 *
 * REQUISITO: el Denunciante debe tener un tokenFcm registrado.
 * Este token lo genera la app Flutter al iniciar sesión y debe
 * enviarse al backend para almacenarlo en la tabla denunciantes.
 *
 * Si el denunciante no tiene token (ej: primera instalación, token expirado),
 * la notificación se omite con log de advertencia — no lanza excepción
 * para no romper el flujo de negocio.
 */

public class NotificacionFirebaseAdapter implements NotificacionPort{
 
    private final FirebaseMessaging firebaseMessaging;
 
    public NotificacionFirebaseAdapter(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }
 
    @Override
    public void notificarDenunciante(Denunciante denunciante, String mensaje) {
 
        if (!denunciante.tieneTokenFcm()) {
            System.out.printf(
                "[FCM] Denunciante %s sin tokenFcm — notificación omitida%n",
                denunciante.getNombre());
            return;
        }
 
        try {
            Message fcmMessage = Message.builder()
                .setNotification(Notification.builder()
                    .setTitle("Callsos — Actualización")
                    .setBody(mensaje)
                    .build())
                .setToken(denunciante.getTokenFcm())
                .build();
 
            String response = firebaseMessaging.send(fcmMessage);
            System.out.printf("[FCM] Enviado a %s: %s%n",
                denunciante.getNombre(), response);
 
        } catch (FirebaseMessagingException e) {
            // Log del error pero no relanzar — la notificación es complementaria,
            // no debe romper el flujo principal de negocio
            System.err.printf("[FCM] Error al notificar a %s: %s%n",
                denunciante.getNombre(), e.getMessage());
        }
    }
}
