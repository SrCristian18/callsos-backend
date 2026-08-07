/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para el registro de un AGENTE mediante token de invitación. */
public class RegistroAgenteRequest {

    @NotBlank(message = "El token de invitación es obligatorio")
    private String token;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El celular es obligatorio")
    private String telefono;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "Debes confirmar la contraseña")
    private String confirmarPassword;

    public String getToken()             { return token; }
    public String getNombre()            { return nombre; }
    public String getTelefono()          { return telefono; }
    public String getUsername()          { return username; }
    public String getPassword()          { return password; }
    public String getConfirmarPassword() { return confirmarPassword; }
}
