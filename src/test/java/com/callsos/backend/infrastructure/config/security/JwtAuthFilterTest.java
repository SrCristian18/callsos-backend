/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Épica 4 (ruta técnica) — "Test unitario de JwtService / JwtAuthFilter".
 *
 * JwtService se mockea: este test verifica el CONTRATO del filtro
 * (cómo reacciona a distintos headers Authorization y qué deja en el
 * SecurityContext), no el algoritmo de firma JWT en sí (eso ya lo
 * cubre JwtServiceTest).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService);
    }

    @AfterEach
    void limpiarContexto() {
        // El filtro escribe en un ThreadLocal estático — hay que limpiarlo
        // entre tests para que uno no contamine al siguiente.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sin header Authorization, no autentica pero continúa la cadena")
    void sinHeaderAuthorization() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Header sin prefijo 'Bearer ' se ignora y continúa la cadena")
    void headerSinPrefijoBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic algunascosas==");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Token Bearer inválido no autentica pero continúa la cadena")
    void tokenBearerInvalido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtService.esValido("token-invalido")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extraerUserId(any());
        verify(jwtService, never()).extraerRol(any());
    }

    @Test
    @DisplayName("Token Bearer válido registra Authentication en el SecurityContext")
    void tokenBearerValidoAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.esValido("token-valido")).thenReturn(true);
        when(jwtService.extraerUserId("token-valido")).thenReturn("den-001");
        when(jwtService.extraerRol("token-valido")).thenReturn("DENUNCIANTE");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals("den-001", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_DENUNCIANTE")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("El rol se antepone con 'ROLE_' para calzar con hasRole() de Spring Security")
    void prefijoRoleEnLaAutoridad() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.esValido("token-valido")).thenReturn(true);
        when(jwtService.extraerUserId("token-valido")).thenReturn("usr-001");
        when(jwtService.extraerRol("token-valido")).thenReturn("COMANDO");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_COMANDO", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Siempre continúa la cadena de filtros, incluso con token válido")
    void siempreContinuaLaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.esValido("token-valido")).thenReturn(true);
        when(jwtService.extraerUserId("token-valido")).thenReturn("den-001");
        when(jwtService.extraerRol("token-valido")).thenReturn("DENUNCIANTE");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
