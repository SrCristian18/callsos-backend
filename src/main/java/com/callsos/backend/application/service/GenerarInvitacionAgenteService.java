/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.in.GenerarInvitacionAgentePort;
import com.callsos.backend.domain.port.out.InvitacionAgenteRepositoryPort;

/**
 * Caso de uso: COMANDO genera un token de invitación para registrar un agente.
 */
public class GenerarInvitacionAgenteService implements GenerarInvitacionAgentePort {

    private final InvitacionAgenteRepositoryPort invitacionRepository;

    public GenerarInvitacionAgenteService(InvitacionAgenteRepositoryPort invitacionRepository) {
        this.invitacionRepository = invitacionRepository;
    }

    @Override
    public InvitacionAgente ejecutar(String unidadPolicialId, String comandoActorId) {
        InvitacionAgente invitacion = InvitacionAgente.generar(unidadPolicialId, comandoActorId);
        invitacionRepository.guardar(invitacion);
        return invitacion;
    }
}
