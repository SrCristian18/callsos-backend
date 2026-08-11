/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config.security;

/**
 *
 * @author LENOVO
 */
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * Autenticación JWT para el canal STOMP (WebSocket).
 *
 * Por qué existe este interceptor:
 *   SecurityConfig tiene "/ws/**".permitAll() porque el handshake HTTP inicial
 *   de WebSocket no puede llevar un header Authorization de forma confiable
 *   en todos los clientes/navegadores (limitación conocida del protocolo).
 *   Por eso la autenticación real se hace acá, en el primer frame STOMP
 *   (CONNECT), que sí puede llevar headers custom.
 *
 * Sin este interceptor, "/ws/**".permitAll() dejaba el canal COMPLETAMENTE
 * abierto: cualquiera podía conectarse, suscribirse a cualquier topic y
 * publicar ubicaciones falsas sin ninguna credencial.
 *
 * El cliente Flutter debe enviar el JWT en el frame CONNECT así:
 *   StompConfig.headers = {'Authorization': 'Bearer <token>'}
 *
 * Frames STOMP posteriores al CONNECT (SEND, SUBSCRIBE) heredan el Principal
 * ya autenticado de la sesión STOMP, no hace falta revalidar en cada uno.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException(
                    "Conexion WebSocket rechazada: falta header Authorization");
            }

            String token = authHeader.substring(7);

            if (!jwtService.esValido(token)) {
                throw new MessagingException(
                    "Conexion WebSocket rechazada: JWT invalido o expirado");
            }

            String userId = jwtService.extraerUserId(token);
            String rol    = jwtService.extraerRol(token);

            Principal principal = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
            );

            // Queda disponible en toda la sesion STOMP como accessor.getUser()
            // / Principal en los @MessageMapping siguientes.
            accessor.setUser(principal);
        }

        return message;
    }
}
