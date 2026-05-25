/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 *
 * @author LENOVO
 */

/** DTO de respuesta con el token JWT generado. */
public class AuthResponse {    
    
    private final String token;
    private final String userId;
    private final String rol;
 
    public AuthResponse(String token, String userId, String rol) {
        this.token  = token;
        this.userId = userId;
        this.rol    = rol;
    }
 
    public String getToken()  { return token; }
    public String getUserId() { return userId; }
    public String getRol()    { return rol; }
}
