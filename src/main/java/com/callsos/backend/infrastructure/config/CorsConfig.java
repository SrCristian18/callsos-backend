/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
 
import java.util.List;
 
/**
 * Configuración CORS para la API REST.
 *
 * Sin esta configuración, Flutter Web y cualquier cliente en un origen
 * distinto al del servidor reciben un bloqueo de Same-Origin Policy antes
 * de que el request llegue siquiera a los controllers.
 *
 * La app Flutter mobile (Android/iOS) NO necesita CORS porque no usa
 * el modelo de seguridad de navegador. Sí lo necesita Flutter Web.
 *
 * Orígenes permitidos:
 *   - localhost:* → desarrollo local (Flutter Web, Postman, Swagger)
 *   - El dominio de producción debe agregarse via variable de entorno
 *     CORS_ALLOWED_ORIGINS cuando se desplegue.
 *
 * NOTA: allowedOrigins("*") con allowCredentials(true) es inválido en
 * el estándar CORS — se usan patrones explícitos en su lugar.
 */
@Configuration
public class CorsConfig {
    
    @Value("${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
    private String corsAllowedOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
 
        // Orígenes permitidos — agregar dominio de producción via env var
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",    // desarrollo Flutter Web
            "http://localhost",      // desarrollo flutter web en puerto 80
            "http://127.0.0.1:*",   // desarrollo alternativo con puerto
            "http://127.0.0.1",     // desarrollo alternativo sin puerto (80 por defecto)
           // "${CORS_ALLOWED_ORIGIN:http://localhost:3000}"  // producción
            corsAllowedOrigin //producción
        ));
 
        // Métodos HTTP permitidos por la API
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
 
        // Headers que el cliente puede enviar
        config.setAllowedHeaders(List.of(
            "Authorization",   // JWT Bearer token
            "Content-Type",    // application/json
            "Accept"
        ));
 
        // Headers que el cliente puede leer en la respuesta
        config.setExposedHeaders(List.of("Authorization"));
 
        // Permite enviar cookies y el header Authorization
        config.setAllowCredentials(true);
 
        // Tiempo que el browser cachea el preflight OPTIONS (en segundos)
        config.setMaxAge(3600L);
 
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);
 
        return source;
    }
}
