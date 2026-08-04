package com.callsos.backend.infrastructure.config;

import com.callsos.backend.infrastructure.config.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad con JWT, roles y CORS.
 *
 * Cambios en Fase D:
 *   - CORS integrado via CorsConfigurationSource (CorsConfig.java)
 *   - Rutas /api/v1/ alineadas con los controllers corregidos
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter          = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS: usa CorsConfig.corsConfigurationSource()
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Sin autenticación → 401 (estándar HTTP).
            // Sin este bloque, Spring Security devuelve 403 por defecto
            // para requests sin credenciales, lo que viola RFC 7235
            // (401 = no autenticado, 403 = autenticado pero sin permiso).
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "No autenticado — se requiere JWT válido"
                    )
                )
            )

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
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/*")
                    .hasAnyRole("DENUNCIANTE", "AGENTE", "OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/mis-incidentes")
                    .hasRole("DENUNCIANTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/asignados")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/por-cai")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/denunciantes/*/token")
                    .hasRole("DENUNCIANTE")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}