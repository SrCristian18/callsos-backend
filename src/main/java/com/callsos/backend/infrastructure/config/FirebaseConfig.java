/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
 
import java.io.IOException;


/**
 * Configuración de Firebase Admin SDK.
 *
 * SETUP REQUERIDO (una sola vez):
 *   1. Ir a Firebase Console → Configuración del proyecto → Cuentas de servicio
 *   2. Generar nueva clave privada → descarga serviceAccountKey.json
 *   3. Colocar el archivo en: src/main/resources/serviceAccountKey.json
 *   4. Agregar al .gitignore: serviceAccountKey.json  ← IMPORTANTE, es una credencial
 *
 * En producción (Docker), la variable de entorno FIREBASE_CREDENTIALS_PATH
 * apunta al archivo montado como volumen o secret.
 *
 * FirebaseMessaging es el bean que NotificacionFirebaseAdapter necesita
 * para enviar notificaciones push.
 */
@Configuration
public class FirebaseConfig {
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Evitar inicialización duplicada si Spring recarga el contexto
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
 
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ClassPathResource("serviceAccountKey.json").getInputStream()
        );
 
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build();
 
        return FirebaseApp.initializeApp(options);
    }
 
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
