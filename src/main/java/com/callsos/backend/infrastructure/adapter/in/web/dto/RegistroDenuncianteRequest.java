/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para el registro abierto de un DENUNCIANTE. */
public class RegistroDenuncianteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El documento es obligatorio")
    private String documento;

    @NotBlank(message = "El celular es obligatorio")
    private String telefono;

    // Épica 8 (hallazgo #6, Parte 1): antes NO se recogía en este
    // formulario — Denunciante.correo quedaba hardcodeado en null aun
    // existiendo la columna en BD desde 01_schema.sql. Obligatorio en
    // cuentas NUEVAS; requisito para recuperación de contraseña por
    // email más adelante en la misma épica.
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "Debes confirmar la contraseña")
    private String confirmarPassword;

    public String getNombre()            { return nombre; }
    public String getApellido()          { return apellido; }
    public String getDocumento()         { return documento; }
    public String getTelefono()          { return telefono; }
    public String getCorreo()            { return correo; }
    public String getPassword()          { return password; }
    public String getConfirmarPassword() { return confirmarPassword; }
}