/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

/**
 *
 * @author LENOVO
 */

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
 * Algoritmo: HS256 (HMAC-SHA256) — simétrico, suficiente para este sistema.
 * Para sistemas multi-servicio se recomendaría RS256 (asimétrico).
 *
 * El token incluye:
 *   sub  → ID del usuario (denunciante, agente, etc.)
 *   rol  → RolUsuario (DENUNCIANTE, AGENTE, OPERADOR_CAI, COMANDO)
 *   iat  → issued at
 *   exp  → expiration (24h por defecto)
 */
@Component
public class JwtService {
    
    private final SecretKey key;
    private final long expirationMs;
 
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key          = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }
 
    /** Genera un token JWT para el usuario con el rol dado. */
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
 
    /** Extrae el ID de usuario del token (sub). */
    public String extraerUserId(String token) {
        return parsearClaims(token).getSubject();
    }
 
    /** Extrae el rol del token. */
    public String extraerRol(String token) {
        return (String) parsearClaims(token).get("rol");
    }
 
    /** Valida que el token sea correcto y no haya expirado. */
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
