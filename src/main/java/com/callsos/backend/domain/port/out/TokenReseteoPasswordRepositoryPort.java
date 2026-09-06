/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

import com.callsos.backend.domain.model.TokenReseteoPassword;

import java.util.Optional;

/**
 * Puerto de salida: contrato de persistencia para TokenReseteoPassword.
 * Épica 8 (hallazgo #6, Parte 2). Mismo shape que InvitacionAgenteRepositoryPort.
 */
public interface TokenReseteoPasswordRepositoryPort {

    void guardar(TokenReseteoPassword token);

    Optional<TokenReseteoPassword> buscarPorToken(String token);

    /** Persiste el cambio de estado (usado/fechaUso) tras marcarUsado(). */
    void actualizar(TokenReseteoPassword token);
}