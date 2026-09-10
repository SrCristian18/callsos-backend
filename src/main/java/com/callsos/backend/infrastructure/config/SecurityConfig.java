package com.callsos.backend.infrastructure.config;

import com.callsos.backend.infrastructure.config.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;

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
            //
            // FIX (auditoría AUD-1): tanto el 401 (falta de autenticación)
            // como el 403 (autenticado sin el rol requerido) se resuelven
            // ANTES de llegar a GlobalExceptionHandler, así que sin este
            // fix el cliente recibía el body de error por defecto de
            // Spring Boot ({"timestamp","status","error","path"}, sin
            // "message" por la config por defecto de
            // server.error.include-message=never) en vez del shape
            // ProblemDetail ({"detail": "..."}) que
            // ApiException._extraerDetail (Flutter) espera. No rompía la
            // app — el frontend ya tiene un mensaje de fallback razonable
            // para ambos casos — pero el mensaje explícito de este bloque
            // nunca llegaba a la UI. Se unifica el formato de respuesta.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    escribirProblemDetail(
                        request, response,
                        HttpStatus.UNAUTHORIZED, "No autenticado",
                        "No autenticado — se requiere JWT válido"
                    )
                )
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    escribirProblemDetail(
                        request, response,
                        HttpStatus.FORBIDDEN, "Acceso denegado",
                        "No tienes permisos para realizar esta acción"
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
                // FIX (auditoría AUD-2): esta ruta no tenía ninguna regla
                // explícita — al no matchear el comodín de un solo
                // segmento "/api/v1/incidentes/*" (son 2 segmentos
                // después de "incidentes"), caía en
                // anyRequest().authenticated() SIN restricción de rol, y
                // SimularRecorridoAgenteService.detener() tampoco valida
                // ownership — cualquier usuario autenticado, de
                // cualquier rol, podía detener la simulación de
                // CUALQUIER incidente. Se restringe al mismo rol que
                // puede iniciarla (en-camino?simular=true, arriba).
                // Impacto real limitado (es una utilidad "SOLO PRUEBAS
                // PILOTO", ver IncidenteController), pero el hueco de
                // autorización es real igual.
                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes/*/detener-simulacion")
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

    /**
     * Escribe un body JSON con el mismo shape que {@link
     * com.callsos.backend.infrastructure.adapter.in.web.GlobalExceptionHandler}
     * (RFC 7807 ProblemDetail) para los casos 401/403 resueltos por Spring
     * Security ANTES de llegar a esa clase — ver comentario en
     * {@link #securityFilterChain}.
     *
     * Se construye el JSON a mano (sin ProblemDetail/ObjectMapper) porque
     * todos los valores son literales fijos de este método — nunca
     * interpolan texto proveniente del request — así que no hay riesgo de
     * inyección ni necesidad de un serializador completo para dos campos
     * de texto conocidos.
     */
    private static void escribirProblemDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
            "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d," +
                "\"detail\":\"%s\",\"instance\":\"%s\",\"timestamp\":\"%s\"}",
            title, status.value(), detail,
            request.getRequestURI(), Instant.now()
        ));
    }
}