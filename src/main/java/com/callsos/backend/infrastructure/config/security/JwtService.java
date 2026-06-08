/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Servicio JWT: genera y valida tokens usando JJWT 0.12.x.
 *
 * SEGURIDAD — Fase D:
 *   El secret ya no tiene un valor por defecto hardcodeado.
 *   Si JWT_SECRET no está definido como variable de entorno,
 *   Spring lanza IllegalStateException al arrancar — el sistema
 *   no puede iniciarse con un secret inseguro en producción.
 *
 *   Longitud mínima: 32 caracteres (256 bits para HS256).
 *   El arranque también valida esta longitud mínima.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {

        // Validación en tiempo de arranque — falla rápido si el secret es débil
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET no está configurado. " +
                "Defina la variable de entorno JWT_SECRET con al menos 32 caracteres.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET demasiado corto (" + secret.length() + " chars). " +
                "Mínimo requerido: 32 caracteres (256 bits para HS256).");
        }

        this.key          = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(String userId, String rol) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId)
            .claims(Map.of("rol", rol))
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMs))
            .signWith(key)
            .compact();
    }

    public String extraerUserId(String token) {
        return parsearClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return (String) parsearClaims(token).get("rol");
    }

    public boolean esValido(String token) {
        try {
            Claims claims = parsearClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}