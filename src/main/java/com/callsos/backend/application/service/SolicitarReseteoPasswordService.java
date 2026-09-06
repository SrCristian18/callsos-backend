/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.TokenReseteoPassword;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.in.SolicitarReseteoPasswordPort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.EnviarCorreoPort;
import com.callsos.backend.domain.port.out.TokenReseteoPasswordRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;

import java.util.Optional;

/**
 * Épica 8 (hallazgo #6, Parte 2): implementación de SolicitarReseteoPasswordPort.
 *
 * Busca el correo en las 3 tablas con correo (denunciantes, agentes,
 * unidades_policiales — COMANDO no tiene tabla propia ni correo hoy, ver
 * Parte 1 de este hallazgo) en ese orden. El actorId encontrado ES el
 * mismo actor_id que usuarios.actor_id — no hace falta ninguna otra
 * traducción para generar el token.
 *
 * Si ningún actor tiene ese correo, el método retorna silenciosamente
 * sin generar token ni enviar correo — ver docstring de
 * SolicitarReseteoPasswordPort para por qué (anti-enumeración de cuentas).
 */
public class SolicitarReseteoPasswordService implements SolicitarReseteoPasswordPort {

    private final DenuncianteRepositoryPort denuncianteRepository;
    private final AgenteRepositoryPort agenteRepository;
    private final UnidadPolicialRepositoryPort unidadPolicialRepository;
    private final TokenReseteoPasswordRepositoryPort tokenRepository;
    private final EnviarCorreoPort enviarCorreo;

    public SolicitarReseteoPasswordService(
            DenuncianteRepositoryPort denuncianteRepository,
            AgenteRepositoryPort agenteRepository,
            UnidadPolicialRepositoryPort unidadPolicialRepository,
            TokenReseteoPasswordRepositoryPort tokenRepository,
            EnviarCorreoPort enviarCorreo) {
        this.denuncianteRepository    = denuncianteRepository;
        this.agenteRepository         = agenteRepository;
        this.unidadPolicialRepository = unidadPolicialRepository;
        this.tokenRepository          = tokenRepository;
        this.enviarCorreo             = enviarCorreo;
    }

    @Override
    public void ejecutar(String correo) {
        if (correo == null || correo.isBlank()) {
            return;
        }

        Optional<String> actorId = buscarActorIdPorCorreo(correo);
        if (actorId.isEmpty()) {
            // No revelar que el correo no existe — ver docstring del puerto.
            return;
        }

        TokenReseteoPassword token = TokenReseteoPassword.generar(actorId.get());
        tokenRepository.guardar(token);

        enviarCorreo.enviar(
            correo,
            "CallSOS — Recuperación de contraseña",
            """
            Recibimos una solicitud para restablecer tu contraseña de CallSOS.

            Tu código de verificación es: %s

            Este código vence en %d minutos y solo puede usarse una vez.

            Si no solicitaste este cambio, podés ignorar este correo — tu \
            contraseña actual sigue siendo válida.
            """.formatted(token.getToken(), TokenReseteoPassword.DURACION_MINUTOS_DEFECTO)
        );
    }

    private Optional<String> buscarActorIdPorCorreo(String correo) {
        Optional<Denunciante> denunciante = denuncianteRepository.buscarPorCorreo(correo);
        if (denunciante.isPresent()) {
            return Optional.of(denunciante.get().getId());
        }

        Optional<Agente> agente = agenteRepository.buscarPorCorreo(correo);
        if (agente.isPresent()) {
            return Optional.of(agente.get().getId());
        }

        Optional<UnidadPolicial> unidad = unidadPolicialRepository.buscarPorCorreo(correo);
        if (unidad.isPresent()) {
            return Optional.of(unidad.get().getId());
        }

        return Optional.empty();
    }
}