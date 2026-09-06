/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para POST /auth/resetear-password.
 * Épica 8 (hallazgo #6, Parte 2).
 */
public class ResetearPasswordRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String nuevaPassword;

    @NotBlank(message = "Debes confirmar la nueva contraseña")
    private String confirmarPassword;

    public String getToken()             { return token; }
    public String getNuevaPassword()     { return nuevaPassword; }
    public String getConfirmarPassword() { return confirmarPassword; }
}