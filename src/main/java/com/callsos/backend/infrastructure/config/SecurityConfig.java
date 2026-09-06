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

                // ── Incidentes: rutas ESPECÍFICAS primero ──────────────────
                // IMPORTANTE: Spring Security usa la PRIMERA regla que hace
                // match (no evalúa todas). Como "/api/v1/incidentes/*" (un
                // solo comodín) coincide igual con un {id} real que con
                // "/mis-incidentes", "/asignados", "/por-cai",
                // "/por-estado" o "/derivados", estas rutas de colección
                // deben declararse ANTES que el comodín genérico de {id},
                // o su restricción de rol nunca se aplica (bug ya
                // presente: las 3 reglas de abajo estaban después del
                // comodín y por lo tanto eran inalcanzables — cualquier
                // rol autenticado podía usarlas).
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/mis-incidentes")
                    .hasRole("DENUNCIANTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/asignados")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/por-cai")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/por-estado")
                    .hasAnyRole("COMANDO", "OPERADOR_CAI")
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/derivados")
                    .hasRole("COMANDO")
                .requestMatchers(HttpMethod.GET, "/api/v1/cais/*/agentes/disponibles")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.POST, "/api/v1/invitaciones")
                    .hasRole("COMANDO")

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
                // Épica 1 — actualización del tipo de incidente: solo el
                // DENUNCIANTE puede iniciarla. El ownership (que sea EL
                // dueño del incidente, no cualquier denunciante) se valida
                // dentro de ActualizarTipoIncidenteService, no aquí — esta
                // regla solo garantiza el rol correcto.
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/tipo")
                    .hasRole("DENUNCIANTE")
                // Épica 4 — ETA: solo el DENUNCIANTE puede consultarlo (vía
                // REST, complementario al broadcast WS). El ownership (que
                // sea el DUEÑO del incidente) se valida dentro de
                // CalcularEtaService, no aquí — mismo patrón que /tipo.
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/*/eta")
                    .hasRole("DENUNCIANTE")

                // ── Reportes ──────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/reportes/hallazgos")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/reportes/administrativo")
                    .hasAnyRole("OPERADOR_CAI", "COMANDO")

                // ── Auditoría ─────────────────────────────────────────────
                // Épica 2 (fix P7): se amplía a los 4 roles porque el
                // filtrado real por actor (denunciante dueño / agente
                // asignado / CAI propio / comando global) ahora vive
                // dentro de AuditoriaController, no acá. Antes esta regla
                // dejaba fuera a DENUNCIANTE y AGENTE por completo, cuando
                // el requisito es que SÍ puedan ver la auditoría de SUS
                // PROPIOS incidentes.
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/**")
                    .hasAnyRole("DENUNCIANTE", "AGENTE", "OPERADOR_CAI", "COMANDO")

                // ── Incidentes: comodín genérico {id} — SIEMPRE AL FINAL ──
                // Debe ir después de todas las rutas de colección de arriba,
                // nunca antes (ver comentario al inicio de este bloque).
                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes/*")
                    .hasAnyRole("DENUNCIANTE", "AGENTE", "OPERADOR_CAI", "COMANDO")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/denunciantes/*/token")
                    .hasRole("DENUNCIANTE")
                // Épica 5: mismo patrón que el token del denunciante —
                // hasRole confirma el rol, el ownership real (actorId ==
                // {id} del path) se valida dentro del controller.
                .requestMatchers(HttpMethod.PATCH, "/api/v1/agentes/*/token")
                    .hasRole("AGENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/cais/*/token")
                    .hasRole("OPERADOR_CAI")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}