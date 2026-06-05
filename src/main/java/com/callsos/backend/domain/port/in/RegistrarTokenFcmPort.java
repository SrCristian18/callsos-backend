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
 * Puerto de entrada: registrar o actualizar el token FCM de un denunciante.
 *
 * Firebase genera un token único por instalación de la app en el dispositivo.
 * Ese token puede renovarse cuando:
 *   - El usuario reinstala la app
 *   - El token expira por inactividad
 *   - Firebase lo rota automáticamente por seguridad
 *
 * Flutter debe llamar a este puerto cada vez que el SDK de Firebase
 * emita un token nuevo (FirebaseMessaging.instance.onTokenRefresh).
 */
public interface RegistrarTokenFcmPort {
    
    /**
     * @param denuncianteId  ID del denunciante autenticado (viene del JWT)
     * @param tokenFcm       Token FCM emitido por Firebase en el dispositivo
     */
    void ejecutar(String denuncianteId, String tokenFcm);
}
