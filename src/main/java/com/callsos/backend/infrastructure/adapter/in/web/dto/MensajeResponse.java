/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 * DTO de respuesta genérico para endpoints que solo confirman una
 * acción, sin datos adicionales que devolver.
 * Épica 8 (hallazgo #6, Parte 2) — usado por /recuperar-password y
 * /resetear-password.
 */
public record MensajeResponse(String mensaje) {}