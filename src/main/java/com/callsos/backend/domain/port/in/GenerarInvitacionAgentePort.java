/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

import com.callsos.backend.domain.model.InvitacionAgente;

/**
 * Puerto de entrada: COMANDO genera un token de invitación para que un
 * agente se registre, atado a un CAI específico.
 */
public interface GenerarInvitacionAgentePort {

    /**
     * @param unidadPolicialId  CAI al que quedará asignado el agente que use este token
     * @param comandoActorId    actorId de COMANDO que genera la invitación (auditoría)
     */
    InvitacionAgente ejecutar(String unidadPolicialId, String comandoActorId);
}