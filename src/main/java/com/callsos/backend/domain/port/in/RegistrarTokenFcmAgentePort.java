package com.callsos.backend.domain.port.in;

/**
 * Puerto de entrada — Épica 5: registrar o actualizar el token FCM de un
 * agente. Mismo propósito y forma que RegistrarTokenFcmPort (denunciante),
 * separado porque el agregado destino (y su repositorio) es distinto.
 */
public interface RegistrarTokenFcmAgentePort {

    /**
     * @param agenteId  ID del agente autenticado (viene del JWT)
     * @param tokenFcm  Token FCM emitido por Firebase en el dispositivo
     */
    void ejecutar(String agenteId, String tokenFcm);
}
