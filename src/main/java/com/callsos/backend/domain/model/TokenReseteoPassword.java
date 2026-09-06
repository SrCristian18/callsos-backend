/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de un solo uso para recuperar/resetear la contraseña de una cuenta.
 *
 * Épica 8 (hallazgo #6, Parte 2). Mismo patrón exacto que InvitacionAgente
 * (mismo problema de fondo: "token de un solo uso con expiración, atado a
 * un actor concreto"), con dos diferencias deliberadas:
 *   - Expira MUCHO más corto (30 min vs 48h) — un token de reseteo de
 *     password es más sensible que una invitación de registro: si se
 *     filtra (ej. en un log, en un correo reenviado sin querer), la
 *     ventana de exposición debe ser mínima.
 *   - No lleva "creadoPor" — a diferencia de la invitación (que la genera
 *     COMANDO para otra persona), este token lo genera el propio sistema
 *     en respuesta a que alguien afirma ser dueño de `actorId` (dueño de
 *     ese correo). La prueba de titularidad es haber recibido el correo,
 *     no quién lo generó.
 *
 * actorId es el mismo id que usuarios.actor_id (denunciante_id, agente_id
 * o unidad_policial_id según el rol) — no se guarda el rol acá porque no
 * hace falta: ResetearPasswordService solo necesita actorId para
 * localizar la fila en `usuarios` y actualizar su password, sin importar
 * a qué tabla de dominio pertenece.
 */
public class TokenReseteoPassword {

    /** Minutos de validez por defecto — corto a propósito, ver docstring de clase. */
    public static final long DURACION_MINUTOS_DEFECTO = 30;

    private final String token;
    private final String actorId;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaExpiracion;
    private boolean usado;
    private LocalDateTime fechaUso;

    /** Genera un token NUEVO (aleatorio, aún no usado) para el actor indicado. */
    public static TokenReseteoPassword generar(String actorId) {
        LocalDateTime ahora = LocalDateTime.now();
        return new TokenReseteoPassword(
            UUID.randomUUID().toString().replace("-", ""),
            actorId,
            ahora,
            ahora.plusMinutes(DURACION_MINUTOS_DEFECTO),
            false,
            null
        );
    }

    /** Reconstituye un token existente desde persistencia — no dispara efectos de negocio. */
    public static TokenReseteoPassword reconstituir(
            String token, String actorId, LocalDateTime fechaCreacion,
            LocalDateTime fechaExpiracion, boolean usado, LocalDateTime fechaUso) {
        return new TokenReseteoPassword(
            token, actorId, fechaCreacion, fechaExpiracion, usado, fechaUso);
    }

    private TokenReseteoPassword(String token, String actorId, LocalDateTime fechaCreacion,
                                  LocalDateTime fechaExpiracion, boolean usado,
                                  LocalDateTime fechaUso) {
        this.token           = token;
        this.actorId         = actorId;
        this.fechaCreacion   = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.usado           = usado;
        this.fechaUso        = fechaUso;
    }

    /** Vigente = no usado Y no expirado. */
    public boolean estaVigente() {
        return !usado && LocalDateTime.now().isBefore(fechaExpiracion);
    }

    /**
     * Marca el token como usado tras resetear la contraseña exitosamente.
     * @throws IllegalStateException si ya no está vigente (usado o expirado) —
     *         el llamador (ResetearPasswordService) debe validar estaVigente()
     *         ANTES de actualizar la contraseña, pero se revalida acá por
     *         defensividad (invariante de la propia entidad).
     */
    public void marcarUsado() {
        if (!estaVigente()) {
            throw new IllegalStateException(
                "El token de reseteo ya no está vigente (usado o expirado).");
        }
        this.usado    = true;
        this.fechaUso = LocalDateTime.now();
    }

    public String getToken()                  { return token; }
    public String getActorId()                { return actorId; }
    public LocalDateTime getFechaCreacion()    { return fechaCreacion; }
    public LocalDateTime getFechaExpiracion()  { return fechaExpiracion; }
    public boolean isUsado()                   { return usado; }
    public LocalDateTime getFechaUso()         { return fechaUso; }
}