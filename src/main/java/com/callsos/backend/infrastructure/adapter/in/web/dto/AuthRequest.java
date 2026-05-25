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
 
/** DTO de entrada para el endpoint de autenticación. */
public class AuthRequest {

    @NotBlank(message = "El ID de usuario es obligatorio")
    private String userId;
 
    @NotBlank(message = "El rol es obligatorio")
    private String rol;
 
    public String getUserId() { return userId; }
    public String getRol()    { return rol; }
}
