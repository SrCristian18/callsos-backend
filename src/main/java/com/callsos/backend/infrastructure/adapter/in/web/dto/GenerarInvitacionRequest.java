/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO de entrada: COMANDO genera una invitación para un CAI específico. */
public class GenerarInvitacionRequest {

    @NotBlank(message = "El CAI (unidadPolicialId) es obligatorio")
    private String unidadPolicialId;

    public String getUnidadPolicialId() { return unidadPolicialId; }
}