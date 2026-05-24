/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
 
/**
 * Configuración de Spring Security — FASE 0 (permisiva para desarrollo).
 *
 * Estado actual: deshabilita autenticación en todos los endpoints
 * para que el sistema pueda levantarse y probarse sin credenciales.
 *
 * IMPORTANTE — esta configuración es TEMPORAL.
 * En Fase 1 se reemplazará por JWT con roles:
 *   DENUNCIANTE  → puede crear incidentes y consultar los suyos
 *   AGENTE       → puede atender y finalizar incidentes asignados
 *   OPERADOR_CAI → puede asignar agentes
 *   COMANDO      → acceso de lectura a reportes y estadísticas
 *
 * POR QUÉ esta clase es obligatoria aunque sea permisiva:
 *   Sin ella, Spring Security aplica su configuración por defecto que:
 *   - Genera una contraseña aleatoria al arrancar
 *   - Exige HTTP Basic en TODOS los endpoints
 *   - Bloquea completamente la API
 *
 * POR QUÉ sessionless (STATELESS):
 *   El backend es una API REST consumida por Flutter mobile.
 *   Las sesiones HTTP no aplican a clientes móviles.
 *   Cuando se implemente JWT, el token reemplaza la sesión.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF: no aplica a APIs REST stateless
            .csrf(AbstractHttpConfigurer::disable)
 
            // Sin sesión de servidor: cada request es independiente
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 
            // FASE 0: todos los endpoints son públicos
            // FASE 1: reemplazar por .requestMatchers(...).hasRole(...)
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll());
 
        return http.build();
    }
}
