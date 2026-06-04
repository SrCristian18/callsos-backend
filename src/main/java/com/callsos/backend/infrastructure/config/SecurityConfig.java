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
 * Matriz de acceso completa — rutas /api/v1/:
 * ┌──────────────────────────────────────────────┬───────────────────────────────┐
 * │ Endpoint                                     │ Roles                         │
 * ├──────────────────────────────────────────────┼───────────────────────────────┤
 * │ POST /api/v1/auth/login                      │ Público                       │
 * │ /ws/**                                       │ Público (auth en STOMP)       │
 * │ /swagger-ui/**, /v3/api-docs/**              │ Público                       │
 * │ POST /api/v1/incidentes                      │ DENUNCIANTE                   │
 * │ GET  /api/v1/incidentes/{id}/estado          │ todos los roles               │
 * │ PATCH /api/v1/incidentes/{id}/estado         │ OPERADOR_CAI, COMANDO         │
 * │ PATCH /api/v1/incidentes/{id}/derivar        │ COMANDO, OPERADOR_CAI         │
 * │ PATCH /api/v1/incidentes/{id}/asignar        │ OPERADOR_CAI, COMANDO         │
 * │ PATCH /api/v1/incidentes/{id}/en-camino      │ AGENTE                        │
 * │ PATCH /api/v1/incidentes/{id}/atender        │ AGENTE                        │
 * │ PATCH /api/v1/incidentes/{id}/evaluar        │ AGENTE                        │
 * │ PATCH /api/v1/incidentes/{id}/cancelar       │ DENUNCIANTE, COMANDO          │
 * │ POST  /api/v1/reportes/hallazgos             │ AGENTE                        │
 * │ POST  /api/v1/reportes/administrativo        │ OPERADOR_CAI, COMANDO         │
 * │ GET   /api/v1/auditoria/**                   │ OPERADOR_CAI, COMANDO         │
 * │ PATCH /api/v1/denunciantes/{id}/token        │ DENUNCIANTE                   │
 * └──────────────────────────────────────────────┴───────────────────────────────┘
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
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
 
                // ── Incidentes ────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,  "/api/v1/incidentes")
                    .hasRole("DENUNCIANTE")
                .requestMatchers(HttpMethod.GET,   "/api/v1/incidentes/*/estado")
                    .hasAnyRole("DENUNCIANTE", "AGENTE", "OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/estado")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/derivar")
                    .hasAnyRole("COMANDO", "OPERADOR_CAI")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/asignar")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/en-camino")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/atender")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/evaluar")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/cancelar")
                    .hasAnyRole("DENUNCIANTE", "COMANDO")
 
                // ── Reportes ──────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/reportes/hallazgos")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/reportes/administrativo")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
 
                // ── Auditoría ─────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/**")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
 
                // ── Denunciante ───────────────────────────────────────────
                .requestMatchers(HttpMethod.PATCH, "/api/v1/denunciantes/*/token")
                    .hasRole("DENUNCIANTE")
 
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);
 
        return http.build();
    }
}