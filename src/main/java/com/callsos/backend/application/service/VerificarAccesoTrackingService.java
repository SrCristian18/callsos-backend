/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.port.in.VerificarAccesoTrackingPort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;

/**
 * Caso de uso: resuelve la matriz de autorización del tracking GPS
 * (Épica 3 — fix P6, la regla de seguridad más crítica del sistema:
 * el denunciante NUNCA puede ver la ubicación del agente).
 *
 * Vive en application/service (no en domain/service) porque necesita
 * consultar AgenteRepositoryPort — resolver "¿este agente pertenece a
 * mi CAI?" requiere I/O (el dominio Agente no carga su unidadPolicialId
 * en memoria, ver AgenteRepositoryPort.buscarUnidadDeAgente), y el
 * dominio no debe depender de puertos de salida.
 */
public class VerificarAccesoTrackingService implements VerificarAccesoTrackingPort {

    private final AgenteRepositoryPort agenteRepository;

    public VerificarAccesoTrackingService(AgenteRepositoryPort agenteRepository) {
        this.agenteRepository = agenteRepository;
    }

    @Override
    public boolean puedeAcceder(String agenteId, String actorId, String rol) {
        if (agenteId == null || actorId == null || rol == null) return false;

        return switch (rol) {
            case "AGENTE" -> agenteId.equals(actorId);
            case "COMANDO" -> true;
            case "OPERADOR_CAI" -> agenteRepository.buscarUnidadDeAgente(agenteId)
                .map(unidadId -> unidadId.equals(actorId))
                .orElse(false);
            // DENUNCIANTE y cualquier otro rol: SIEMPRE denegado.
            // No usar default -> true jamás en este switch.
            default -> false;
        };
    }
}