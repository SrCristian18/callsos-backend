/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.in.ConsultarAgentesDisponiblesPorCaiPort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;

import java.util.List;

/**
 * Caso de uso: listar agentes DISPONIBLES de un CAI.
 *
 * FIX: resuelve el Gap 3 de deuda_backend.md — reutiliza
 * AgenteRepositoryPort.obtenerDisponiblesPorUnidad(), que ya existía
 * pero solo se usaba internamente dentro de AsignarAgenteService para
 * la auto-asignación. Este caso de uso expone ese mismo listado hacia
 * afuera, para que el operador del CAI pueda verlo antes de asignar.
 */
public class ConsultarAgentesDisponiblesPorCaiService
        implements ConsultarAgentesDisponiblesPorCaiPort {

    private final AgenteRepositoryPort agenteRepository;

    public ConsultarAgentesDisponiblesPorCaiService(
            AgenteRepositoryPort agenteRepository) {
        this.agenteRepository = agenteRepository;
    }

    @Override
    public List<Agente> ejecutar(String caiId) {
        return agenteRepository.obtenerDisponiblesPorUnidad(caiId);
    }
}
