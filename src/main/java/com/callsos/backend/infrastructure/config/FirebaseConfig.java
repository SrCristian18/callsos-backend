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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
 *
 * @ConditionalOnProperty: cuando firebase.enabled=false (perfil de test),
 * estos beans se omiten completamente — evita la IOException por
 * serviceAccountKey.json no encontrado en el classpath de tests.
 */

// Para cuando no es posible usar Firebase para desacoplarlo del sistema
// encender o apagar firebase en producción

@Configuration
@ConditionalOnProperty(
    name = "firebase.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {

        // Evitar inicialización duplicada si Spring recarga el contexto
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Path path = Paths.get(credentialsPath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException(
                "No se encontró el archivo de credenciales de Firebase: "
                + credentialsPath
            );
        }

        try (InputStream serviceAccount = Files.newInputStream(path)) {

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            return FirebaseApp.initializeApp(options);
        }

        /*
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ClassPathResource("serviceAccountKey.json").getInputStream()
        );
        */
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}