/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — "Test unitario de JwtService / JwtAuthFilter".
 *
 * No se mockea la librería JJWT: se instancia un JwtService real con un
 * secret de prueba (>= 32 chars) para verificar el comportamiento real de
 * firma/verificación, no una simulación de él.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    // Secret de prueba, 64 chars — cumple el mínimo de 32 exigido en el constructor.
    private static final String SECRET_VALIDO =
        "callsos-test-secret-key-only-for-unit-tests-not-production-1234";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_VALIDO, 3_600_000L); // 1 hora
    }

    @Test
    @DisplayName("generarToken produce un JWT que esValido() acepta")
    void generarTokenYValidarlo() {
        String token = jwtService.generarToken("den-001", "DENUNCIANTE");

        assertNotNull(token);
        assertTrue(jwtService.esValido(token));
    }

    @Test
    @DisplayName("extraerUserId devuelve el subject usado al generar el token")
    void extraerUserId() {
        String token = jwtService.generarToken("ag-real-001", "AGENTE");

        assertEquals("ag-real-001", jwtService.extraerUserId(token));
    }

    @Test
    @DisplayName("extraerRol devuelve el claim 'rol' usado al generar el token")
    void extraerRol() {
        String token = jwtService.generarToken("usr-001", "COMANDO");

        assertEquals("COMANDO", jwtService.extraerRol(token));
    }

    @Test
    @DisplayName("esValido devuelve false para un token expirado")
    void tokenExpiradoNoEsValido() {
        // expirationMs negativo -> el token nace ya vencido
        JwtService servicioExpira = new JwtService(SECRET_VALIDO, -1_000L);
        String tokenExpirado = servicioExpira.generarToken("den-001", "DENUNCIANTE");

        assertFalse(servicioExpira.esValido(tokenExpirado));
    }

    @Test
    @DisplayName("esValido devuelve false para un token malformado")
    void tokenMalformadoNoEsValido() {
        assertFalse(jwtService.esValido("esto-no-es-un-jwt"));
    }

    @Test
    @DisplayName("esValido devuelve false para un token vacío")
    void tokenVacioNoEsValido() {
        assertFalse(jwtService.esValido(""));
    }

    @Test
    @DisplayName("esValido devuelve false si el token fue firmado con otra clave")
    void tokenFirmadoConOtraClaveNoEsValido() {
        JwtService otroServicio = new JwtService(
            "otra-clave-completamente-distinta-de-32-chars-o-mas-xx", 3_600_000L);
        String tokenAjeno = otroServicio.generarToken("den-001", "DENUNCIANTE");

        // jwtService (clave distinta) no debe poder verificar la firma
        assertFalse(jwtService.esValido(tokenAjeno));
    }

    @Test
    @DisplayName("extraerUserId sobre un token con firma inválida lanza JwtException")
    void extraerUserIdConFirmaInvalidaLanzaExcepcion() {
        JwtService otroServicio = new JwtService(
            "otra-clave-completamente-distinta-de-32-chars-o-mas-xx", 3_600_000L);
        String tokenAjeno = otroServicio.generarToken("den-001", "DENUNCIANTE");

        assertThrows(io.jsonwebtoken.JwtException.class,
            () -> jwtService.extraerUserId(tokenAjeno));
    }

    @Test
    @DisplayName("Constructor lanza IllegalStateException si el secret es null")
    void secretNuloLanzaExcepcion() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> new JwtService(null, 3_600_000L));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("Constructor lanza IllegalStateException si el secret está en blanco")
    void secretEnBlancoLanzaExcepcion() {
        assertThrows(IllegalStateException.class,
            () -> new JwtService("   ", 3_600_000L));
    }

    @Test
    @DisplayName("Constructor lanza IllegalStateException si el secret tiene menos de 32 caracteres")
    void secretCortoLanzaExcepcion() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> new JwtService("corto", 3_600_000L));
        assertTrue(ex.getMessage().contains("demasiado corto"));
    }

    @Test
    @DisplayName("Constructor acepta un secret de exactamente 32 caracteres")
    void secretDeLongitudMinimaEsAceptado() {
        JwtService servicio = new JwtService("a".repeat(32), 3_600_000L);
        String token = servicio.generarToken("den-001", "DENUNCIANTE");

        assertTrue(servicio.esValido(token));
    }
}