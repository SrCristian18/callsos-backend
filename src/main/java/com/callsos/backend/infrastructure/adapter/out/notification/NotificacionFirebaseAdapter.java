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
import org.springframework.stereotype.Component;
 
/**
 * Adaptador de salida: implementa NotificacionPuerto usando Firebase Cloud Messaging.
 *
 * Actualmente es un stub — la integración real con el SDK de Firebase
 * se completa cuando se agregue la dependencia 'firebase-admin' al pom.xml
 * y se configure el archivo de credenciales serviceAccountKey.json.
 *
 * Dependencia pendiente en pom.xml:
 *   <dependency>
 *     <groupId>com.google.firebase</groupId>
 *     <artifactId>firebase-admin</artifactId>
 *     <version>9.2.0</version>
 *   </dependency>
 */
@Component
public class NotificacionFirebaseAdapter implements NotificacionPort{
 
    /*
     * FirebaseApp se inicializa en una clase @Configuration separada.
     * Se inyectará aquí cuando la dependencia esté disponible:
     *
     * private final FirebaseMessaging firebaseMessaging;
     *
     * public NotificacionFirebaseAdapter(FirebaseMessaging firebaseMessaging) {
     *     this.firebaseMessaging = firebaseMessaging;
     * }
     */
    
    @Override
    public void notificarDenunciante(Denunciante denunciante, String mensaje) {
        /*
         * Implementación real con Firebase:
         *
         * Message fcmMessage = Message.builder()
         *     .setNotification(Notification.builder()
         *         .setTitle("Actualización de su incidente")
         *         .setBody(mensaje)
         *         .build())
         *     .setToken(denunciante.getTokenFcm())   // pendiente: agregar tokenFcm a Denunciante
         *     .build();
         * firebaseMessaging.send(fcmMessage);
         */
 
        // Stub temporal: log en consola hasta integrar Firebase
        System.out.printf("[NOTIFICACION] -> Denunciante: %s | Mensaje: %s%n",
            denunciante.getNombre(), mensaje);
    }
}
