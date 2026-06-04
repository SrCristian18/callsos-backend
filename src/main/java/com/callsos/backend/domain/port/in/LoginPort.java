/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

/**
 * Puerto de entrada: autenticar un usuario y emitir un JWT.
 *
 * Recibe username + password en texto plano.
 * Retorna el JWT firmado si las credenciales son válidas.
 * Lanza IllegalArgumentException si el usuario no existe o la contraseña no coincide.
 */
public interface LoginPort {
    
    LoginResultado ejecutar(String username, String password);
 
    /**
     * Resultado del login exitoso.
     *
     * @param token    JWT firmado listo para enviar al cliente
     * @param actorId  ID del modelo de negocio (denunciante_id, agente_id…)
     *                 que Flutter necesita para llamar a otros endpoints
     * @param rol      Rol del usuario autenticado
     */
    record LoginResultado(String token, String actorId, String rol) {}
}
