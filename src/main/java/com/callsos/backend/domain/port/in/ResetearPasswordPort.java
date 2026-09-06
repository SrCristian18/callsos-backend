/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 * Caso de uso: completar el reseteo de contraseña con un token vigente.
 * Épica 8 (hallazgo #6, Parte 2).
 */
public interface ResetearPasswordPort {

    /**
     * @param token              token recibido por correo
     * @param nuevaPassword      nueva contraseña en texto plano (se hashea acá)
     * @param confirmarPassword  debe coincidir con nuevaPassword
     * @throws IllegalStateException si las contraseñas no coinciden, o si
     *         el token no existe / ya no está vigente (usado o expirado)
     */
    void ejecutar(String token, String nuevaPassword, String confirmarPassword);
}