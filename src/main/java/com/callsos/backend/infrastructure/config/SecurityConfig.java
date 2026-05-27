/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.infrastructure.config.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad con JWT y roles.
 *
 * Fase 2: se agregó /ws/** como endpoint público para WebSocket.
 * La autenticación en WebSocket se gestiona en el handshake STOMP
 * (el cliente envía el JWT en el header Authorization del CONNECT frame).
 *
 * Matriz de acceso completa:
 * ┌─────────────────────────────────────────┬──────────────────────────────┐
 * │ Endpoint                                │ Roles                        │
 * ├─────────────────────────────────────────┼──────────────────────────────┤
 * │ POST /api/auth/token                    │ Público                      │
 * │ /ws/**                                  │ Público (auth en STOMP)      │
 * │ /swagger-ui/**, /v3/api-docs/**         │ Público                      │
 * │ POST /api/incidentes                    │ DENUNCIANTE                  │
 * │ GET  /api/incidentes/{id}/estado        │ DENUNCIANTE,AGENTE,OPERADOR  │
 * │ PATCH /api/incidentes/{id}/derivar      │ COMANDO, OPERADOR_CAI        │
 * │ PATCH /api/incidentes/{id}/asignar      │ OPERADOR_CAI, COMANDO        │
 * │ PATCH /api/incidentes/{id}/en-camino    │ AGENTE                       │
 * │ PATCH /api/incidentes/{id}/atender      │ AGENTE                       │
 * │ PATCH /api/incidentes/{id}/evaluar      │ AGENTE                       │
 * │ PATCH /api/incidentes/{id}/cancelar     │ DENUNCIANTE, COMANDO         │
 * │ POST  /api/reportes/hallazgos           │ AGENTE                       │
 * │ POST  /api/reportes/administrativo      │ OPERADOR_CAI, COMANDO        │
 * └─────────────────────────────────────────┴──────────────────────────────┘
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthFilter jwtAuthFilter;
 
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }
 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 
            .authorizeHttpRequests(auth -> auth
 
                // ── Públicos ──────────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()          // WebSocket
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
 
                // ── Incidentes ────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,  "/api/incidentes")
                    .hasRole("DENUNCIANTE")
                .requestMatchers(HttpMethod.GET,   "/api/incidentes/*/estado")
                    .hasAnyRole("DENUNCIANTE", "AGENTE", "OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/incidentes/*/derivar")
                    .hasAnyRole("COMANDO", "OPERADOR_CAI")
                .requestMatchers(HttpMethod.PATCH, "/api/incidentes/*/asignar")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/incidentes/*/en-camino")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/incidentes/*/atender")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/incidentes/*/evaluar")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/incidentes/*/cancelar")
                    .hasAnyRole("DENUNCIANTE", "COMANDO")
 
                // ── Reportes ──────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/reportes/hallazgos")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/reportes/administrativo")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
 
                // Cualquier otro endpoint requiere autenticación válida
                .anyRequest().authenticated()
            )
 
            // Registrar el filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
 
        return http.build();
    }
}
