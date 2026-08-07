/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import com.callsos.backend.domain.model.InvitacionAgente;

import java.time.LocalDateTime;

/**
 * DTO de salida de una invitación recién generada.
 * Comando comparte "token" con el agente por un canal fuera de la app
 * (verbal, mensaje interno) — el sistema no lo distribuye automáticamente.
 */
public class InvitacionResponse {

    private final String token;
    private final String unidadPolicialId;
    private final LocalDateTime fechaExpiracion;

    public InvitacionResponse(String token, String unidadPolicialId,
                               LocalDateTime fechaExpiracion) {
        this.token            = token;
        this.unidadPolicialId = unidadPolicialId;
        this.fechaExpiracion  = fechaExpiracion;
    }

    public static InvitacionResponse desde(InvitacionAgente invitacion) {
        return new InvitacionResponse(
            invitacion.getToken(),
            invitacion.getUnidadPolicialId(),
            invitacion.getFechaExpiracion()
        );
    }

    public String getToken()                     { return token; }
    public String getUnidadPolicialId()           { return unidadPolicialId; }
    public LocalDateTime getFechaExpiracion()     { return fechaExpiracion; }
}
