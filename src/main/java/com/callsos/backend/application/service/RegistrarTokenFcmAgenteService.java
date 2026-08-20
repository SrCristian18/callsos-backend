package com.callsos.backend.application.service;

import com.callsos.backend.domain.port.in.RegistrarTokenFcmAgentePort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;

/**
 * Caso de uso — Épica 5: registrar o actualizar el token FCM de un agente.
 * Mismo patrón que RegistrarTokenFcmService (denunciante).
 *
 * Reutiliza buscarUnidadDeAgente() (Épica 3) como chequeo de existencia —
 * Optional.empty() significa "el agente no existe", sin necesidad de un
 * método buscarPorId() nuevo solo para esta validación.
 */
public class RegistrarTokenFcmAgenteService implements RegistrarTokenFcmAgentePort {

    private final AgenteRepositoryPort agenteRepository;

    public RegistrarTokenFcmAgenteService(AgenteRepositoryPort agenteRepository) {
        this.agenteRepository = agenteRepository;
    }

    @Override
    public void ejecutar(String agenteId, String tokenFcm) {

        agenteRepository.buscarUnidadDeAgente(agenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Agente no encontrado: " + agenteId));

        agenteRepository.actualizarTokenFcm(agenteId, tokenFcm);
    }
}
