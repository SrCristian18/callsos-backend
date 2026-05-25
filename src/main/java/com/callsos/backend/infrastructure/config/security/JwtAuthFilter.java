/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

/**
 *
 * @author LENOVO
 */


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
 
import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT: intercepta cada request, extrae y valida el token Bearer,
 * y registra la autenticación en el SecurityContext.
 *
 * Si el token es inválido o no existe, la request continúa sin autenticación
 * y Spring Security la rechaza si el endpoint requiere autenticación.
 *
 * Flujo:
 *   Request → JwtAuthFilter → SecurityContext → Controller
 *
 * Header esperado: Authorization: Bearer <token>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter{
    
    private final JwtService jwtService;
 
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
 
        String authHeader = request.getHeader("Authorization");
 
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
 
            if (jwtService.esValido(token)) {
                String userId = jwtService.extraerUserId(token);
                String rol    = jwtService.extraerRol(token);
 
                // Registrar autenticación en el contexto de seguridad
                var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
 
        filterChain.doFilter(request, response);
    }
}
