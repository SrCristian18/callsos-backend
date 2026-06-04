/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 *
 * @author LENOVO
 */
import jakarta.validation.constraints.NotBlank;
 
/**
 * DTO de entrada para el endpoint de login.
 * Reemplaza la versión anterior que usaba userId+rol directamente
 * sin verificar credenciales reales.
 */
public class AuthRequest {
 
    @NotBlank(message = "El username es obligatorio")
    private String username;
 
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
 
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
