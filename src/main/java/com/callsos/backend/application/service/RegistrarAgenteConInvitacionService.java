package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.RolUsuario;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.domain.port.in.RegistrarAgenteConInvitacionPort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.InvitacionAgenteRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import com.callsos.backend.infrastructure.config.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: registro de AGENTE mediante token de invitación.
 *
 * FIX: resuelve el registro de agente de la Épica 2 — el agente NUNCA
 * elige su propio CAI, sale de la invitación ya validada.
 *
 * @Transactional: escribe en TRES tablas (agentes + usuarios +
 * invitaciones_agente). Si cualquier paso falla, todo se revierte —
 * en particular, la invitación NO debe quedar marcada como usada si la
 * creación del agente o del usuario fallan después.
 */
public class RegistrarAgenteConInvitacionService implements RegistrarAgenteConInvitacionPort {

    private final InvitacionAgenteRepositoryPort invitacionRepository;
    private final AgenteRepositoryPort agenteRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegistrarAgenteConInvitacionService(
            InvitacionAgenteRepositoryPort invitacionRepository,
            AgenteRepositoryPort agenteRepository,
            UsuarioRepositoryPort usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.invitacionRepository = invitacionRepository;
        this.agenteRepository     = agenteRepository;
        this.usuarioRepository    = usuarioRepository;
        this.passwordEncoder      = passwordEncoder;
        this.jwtService           = jwtService;
    }

    @Override
    @Transactional
    public LoginPort.LoginResultado ejecutar(RegistroAgenteData datos) {

        if (!datos.password().equals(datos.confirmarPassword())) {
            throw new IllegalStateException("Las contraseñas no coinciden.");
        }

        InvitacionAgente invitacion = invitacionRepository
            .buscarPorToken(datos.token())
            .orElseThrow(() -> new IllegalStateException(
                "Token de invitación inválido."));

        if (!invitacion.estaVigente()) {
            throw new IllegalStateException(
                "Token de invitación expirado o ya utilizado. " +
                "Solicita uno nuevo a tu Comando.");
        }

        if (usuarioRepository.existePorUsername(datos.username())) {
            throw new IllegalStateException(
                "Ya existe una cuenta con ese nombre de usuario.");
        }

        // El CAI viene de la invitación — nunca del cliente.
        String agenteId = UUID.randomUUID().toString();
        Agente agente = new Agente(
            agenteId,
            datos.nombre(),
            null,               // dirección — no se recoge en el registro
            null,               // ubicación — aún no reportó posición
            datos.telefono()
        );
        agenteRepository.guardar(agente, invitacion.getUnidadPolicialId());

        String passwordHash = passwordEncoder.encode(datos.password());
        usuarioRepository.guardar(
            UUID.randomUUID().toString(),
            datos.username(),
            datos.nombre(),
            passwordHash,
            RolUsuario.AGENTE.name(),
            agenteId
        );

        invitacion.marcarUsado(agenteId);
        invitacionRepository.actualizar(invitacion);

        String token = jwtService.generarToken(agenteId, RolUsuario.AGENTE.name());
        return new LoginPort.LoginResultado(
            token, agenteId, RolUsuario.AGENTE.name(), datos.nombre());
    }
}
