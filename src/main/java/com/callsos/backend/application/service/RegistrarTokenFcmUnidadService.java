package com.callsos.backend.application.service;

import com.callsos.backend.domain.port.in.RegistrarTokenFcmUnidadPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;

/**
 * Caso de uso — Épica 5: registrar o actualizar el token FCM de una
 * unidad policial (CAI). Mismo patrón que RegistrarTokenFcmService
 * (denunciante) y RegistrarTokenFcmAgenteService.
 */
public class RegistrarTokenFcmUnidadService implements RegistrarTokenFcmUnidadPort {

    private final UnidadPolicialRepositoryPort unidadPolicialRepository;

    public RegistrarTokenFcmUnidadService(UnidadPolicialRepositoryPort unidadPolicialRepository) {
        this.unidadPolicialRepository = unidadPolicialRepository;
    }

    @Override
    public void ejecutar(String unidadPolicialId, String tokenFcm) {

        unidadPolicialRepository.buscarPorId(unidadPolicialId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unidad policial no encontrada: " + unidadPolicialId));

        unidadPolicialRepository.actualizarTokenFcm(unidadPolicialId, tokenFcm);
    }
}
