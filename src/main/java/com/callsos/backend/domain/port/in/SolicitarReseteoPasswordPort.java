/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 * Caso de uso: solicitar recuperación de contraseña por correo.
 * Épica 8 (hallazgo #6, Parte 2).
 *
 * IMPORTANTE — no revela si el correo existe o no: {@link #ejecutar}
 * nunca lanza excepción ni indica de ninguna forma observable si el
 * correo pertenece a una cuenta real. Esto es deliberado (previene
 * enumeración de cuentas — un atacante no debe poder usar este endpoint
 * para descubrir qué correos están registrados). El controller SIEMPRE
 * responde el mismo mensaje genérico, exista o no la cuenta.
 */
public interface SolicitarReseteoPasswordPort {

    /**
     * @param correo  correo ingresado por quien solicita el reseteo
     */
    void ejecutar(String correo);
}