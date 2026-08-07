/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de invitación para el registro de un Agente.
 *
 * FIX: resuelve el registro de AGENTE de la Épica 2 (deuda_backend.md).
 * Decisión de diseño (confirmada con el equipo): COMANDO genera el token
 * manualmente por cada agente, eligiendo el CAI en ese momento — el
 * agente NUNCA elige su propio CAI ni se autoriza a sí mismo. El token
 * expira corto (48h por defecto) y es de un solo uso.
 *
 * Por qué un token pre-generado y no autorregistro + cola de aprobación:
 *   - El CAI queda sellado en el token desde su creación — no puede ser
 *     manipulado por el cliente que se registra.
 *   - No existen cuentas de agente "a medio crear" en la base de datos:
 *     o el token es válido y la cuenta nace activa, o no existe cuenta.
 *   - No requiere una pantalla de "solicitudes pendientes" en Comando.
 */
public class InvitacionAgente {

    /** Horas de validez por defecto para una invitación nueva. */
    public static final long DURACION_HORAS_DEFECTO = 48;

    private final String token;
    private final String unidadPolicialId;
    private final String creadoPor;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaExpiracion;
    private boolean usado;
    private String usadoPor;
    private LocalDateTime fechaUso;

    /** Genera una invitación NUEVA (token aleatorio, aún no usada). */
    public static InvitacionAgente generar(String unidadPolicialId, String creadoPor) {
        LocalDateTime ahora = LocalDateTime.now();
        return new InvitacionAgente(
            UUID.randomUUID().toString().replace("-", ""),
            unidadPolicialId,
            creadoPor,
            ahora,
            ahora.plusHours(DURACION_HORAS_DEFECTO),
            false,
            null,
            null
        );
    }

    /** Reconstituye una invitación existente desde persistencia — no dispara efectos de negocio. */
    public static InvitacionAgente reconstituir(
            String token, String unidadPolicialId, String creadoPor,
            LocalDateTime fechaCreacion, LocalDateTime fechaExpiracion,
            boolean usado, String usadoPor, LocalDateTime fechaUso) {
        return new InvitacionAgente(token, unidadPolicialId, creadoPor,
            fechaCreacion, fechaExpiracion, usado, usadoPor, fechaUso);
    }

    private InvitacionAgente(String token, String unidadPolicialId, String creadoPor,
                              LocalDateTime fechaCreacion, LocalDateTime fechaExpiracion,
                              boolean usado, String usadoPor, LocalDateTime fechaUso) {
        this.token            = token;
        this.unidadPolicialId = unidadPolicialId;
        this.creadoPor        = creadoPor;
        this.fechaCreacion    = fechaCreacion;
        this.fechaExpiracion  = fechaExpiracion;
        this.usado            = usado;
        this.usadoPor         = usadoPor;
        this.fechaUso         = fechaUso;
    }

    /** Vigente = no usada Y no expirada. */
    public boolean estaVigente() {
        return !usado && LocalDateTime.now().isBefore(fechaExpiracion);
    }

    /**
     * Marca la invitación como usada por el agente recién creado.
     * @throws IllegalStateException si ya no está vigente (usada o expirada) —
     *         el llamador (RegistrarAgenteConInvitacionService) debe validar
     *         estaVigente() ANTES de crear el agente, pero se revalida acá
     *         por defensividad (invariante de la propia entidad).
     */
    public void marcarUsado(String agenteId) {
        if (!estaVigente()) {
            throw new IllegalStateException(
                "La invitación ya no está vigente (usada o expirada).");
        }
        this.usado    = true;
        this.usadoPor = agenteId;
        this.fechaUso = LocalDateTime.now();
    }

    public String getToken()            { return token; }
    public String getUnidadPolicialId() { return unidadPolicialId; }
    public String getCreadoPor()        { return creadoPor; }
    public LocalDateTime getFechaCreacion()   { return fechaCreacion; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public boolean isUsado()            { return usado; }
    public String getUsadoPor()         { return usadoPor; }
    public LocalDateTime getFechaUso()  { return fechaUso; }
}
