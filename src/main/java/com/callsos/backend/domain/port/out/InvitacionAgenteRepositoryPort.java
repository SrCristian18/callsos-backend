/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

import com.callsos.backend.domain.model.InvitacionAgente;

import java.util.Optional;

/** Puerto de salida: persistencia de tokens de invitación para registro de agentes. */
public interface InvitacionAgenteRepositoryPort {

    void guardar(InvitacionAgente invitacion);

    Optional<InvitacionAgente> buscarPorToken(String token);

    /** Persiste los cambios de estado (usado/usadoPor/fechaUso) de una invitación. */
    void actualizar(InvitacionAgente invitacion);
}