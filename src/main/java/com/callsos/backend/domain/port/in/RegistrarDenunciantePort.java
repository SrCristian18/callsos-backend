/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 * Puerto de entrada: autorregistro abierto de un DENUNCIANTE.
 * A diferencia del registro de AGENTE, no requiere ninguna autorización
 * previa — cualquiera puede registrarse como denunciante.
 *
 * NOTA de diseño: no hay campo "username" separado — el mockup original
 * de RegisterDenuncianteView solo recoge nombre/apellido/documento/celular/
 * contraseña, sin un campo de usuario propio. Se usa "documento" como
 * username de login (ya es un identificador único y el denunciante
 * siempre lo tiene a mano), en vez de agregar un campo nuevo a la UI
 * que el diseño original nunca contempló.
 *
 * Devuelve el mismo LoginResultado que LoginPort para poder autologuear
 * al denunciante justo después de registrarse.
 */
public interface RegistrarDenunciantePort {

    LoginPort.LoginResultado ejecutar(RegistroDenuncianteData datos);

    record RegistroDenuncianteData(
        String nombre,
        String apellido,
        String documento,
        String telefono,
        String correo,
        String password,
        String confirmarPassword
    ) {}
}