/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 * Puerto de entrada: registro de AGENTE mediante token de invitación.
 *
 * A diferencia de RegistrarDenunciantePort, este SÍ requiere un campo
 * "username" explícito en el DTO: el mockup original de agente no tenía
 * ningún identificador único reutilizable (no hay cédula/documento en el
 * formulario, y "número de placa" no existe en el modelo de dominio —
 * ver AgenteDisponibleResponse), así que se agrega el campo a la UI.
 *
 * El CAI (unidadPolicialId) NO viene en este DTO — sale de la invitación
 * validada por el token, nunca del cliente.
 */
public interface RegistrarAgenteConInvitacionPort {

    LoginPort.LoginResultado ejecutar(RegistroAgenteData datos);

    record RegistroAgenteData(
        String token,
        String nombre,
        String telefono,
        String correo,
        String username,
        String password,
        String confirmarPassword
    ) {}
}