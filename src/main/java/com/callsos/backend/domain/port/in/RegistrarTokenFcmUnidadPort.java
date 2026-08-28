package com.callsos.backend.domain.port.in;

/**
 * Puerto de entrada — Épica 5: registrar o actualizar el token FCM de una
 * unidad policial (CAI). Mismo propósito y forma que RegistrarTokenFcmPort
 * (denunciante) y RegistrarTokenFcmAgentePort.
 */
public interface RegistrarTokenFcmUnidadPort {

    /**
     * @param unidadPolicialId  ID de la unidad autenticada (viene del JWT)
     * @param tokenFcm          Token FCM emitido por Firebase en el dispositivo
     */
    void ejecutar(String unidadPolicialId, String tokenFcm);
}
