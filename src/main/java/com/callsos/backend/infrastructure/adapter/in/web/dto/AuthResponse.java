/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 *
 * @author LENOVO
 */

/**
 * DTO de respuesta del login exitoso.
 *
 * actorId: el ID que Flutter debe usar para llamar a los demás endpoints
 *   (ej: PATCH /api/v1/denunciantes/{actorId}/token para registrar FCM).
 *   Es el ID del denunciante, agente o CAI según el rol.
 */
public class AuthResponse {
 
    private final String token;
    private final String actorId;
    private final String rol;
 
    public AuthResponse(String token, String actorId, String rol) {
        this.token   = token;
        this.actorId = actorId;
        this.rol     = rol;
    }
 
    public String getToken()   { return token; }
    public String getActorId() { return actorId; }
    public String getRol()     { return rol; }
}
 