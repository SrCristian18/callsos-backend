/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort.UsuarioCredencial;
import com.callsos.backend.infrastructure.config.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
 
/**
 * Caso de uso: autenticar usuario con username + password → JWT.
 *
 * Flujo:
 *   1. Buscar usuario por username en BD (UsuarioRepositoryPort).
 *   2. Si no existe → IllegalArgumentException (mensaje genérico
 *      para no revelar si el username existe o no).
 *   3. Verificar password contra el hash BCrypt almacenado.
 *   4. Si no coincide → IllegalArgumentException (mismo mensaje genérico).
 *   5. Generar JWT con userId = actorId y el rol del usuario.
 *   6. Retornar token + actorId + rol al controlador.
 *
 * POR QUÉ userId = actorId en el JWT:
 *   El JWT se usa como identidad en todos los endpoints siguientes.
 *   Flutter necesita el ID del denunciante/agente para llamar a
 *   PATCH /denunciantes/{id}/token o para identificarse en WebSocket.
 *   actorId es ese ID — no el ID interno de la tabla usuarios.
 */
public class LoginService implements LoginPort{
    
    private static final String ERROR_CREDENCIALES =
        "Username o contraseña incorrectos.";
 
    private final UsuarioRepositoryPort usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
 
    public LoginService(UsuarioRepositoryPort usuarioRepository,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService        = jwtService;
        this.passwordEncoder   = passwordEncoder;
    }
 
    @Override
    public LoginResultado ejecutar(String username, String password) {
 
        UsuarioCredencial credencial = usuarioRepository
            .buscarPorUsername(username)
            .orElseThrow(() -> new IllegalArgumentException(ERROR_CREDENCIALES));
   
        if (!passwordEncoder.matches(password, credencial.password())) {
            throw new IllegalArgumentException(ERROR_CREDENCIALES);
        }
 
        // userId en el JWT = actorId (ID del denunciante/agente/CAI)
        String token = jwtService.generarToken(credencial.actorId(), credencial.rol());
 
        return new LoginResultado(token, credencial.actorId(), credencial.rol());
    }
}
