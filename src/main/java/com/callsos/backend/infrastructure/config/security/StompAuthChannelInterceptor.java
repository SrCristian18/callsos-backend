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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.callsos.backend.domain.port.in.VerificarAccesoTrackingPort;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 *
 * Épica 3 (fix P6): además de autenticar en CONNECT, este interceptor
 * ahora AUTORIZA el comando SUBSCRIBE para el topic de tracking del
 * agente. Antes, "/topic/incidente/{id}/ubicacion" era un broker simple
 * en memoria sin ningún control sobre quién podía suscribirse — cualquier
 * cliente autenticado, sin importar el rol, podía suscribirse manualmente
 * solo con conocer el incidenteId. Eso incluía al propio DENUNCIANTE, lo
 * cual viola la regla de seguridad más importante del sistema: el
 * denunciante nunca puede ver la ubicación del agente. "Ocultar el mapa"
 * en Flutter no alcanza — la autorización real vive acá.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_UBICACION_AGENTE =
        Pattern.compile("^/topic/agente/([^/]+)/ubicacion$");

    private final JwtService jwtService;
    private final VerificarAccesoTrackingPort verificarAccesoTracking;

    public StompAuthChannelInterceptor(JwtService jwtService,
                                       VerificarAccesoTrackingPort verificarAccesoTracking) {
        this.jwtService = jwtService;
        this.verificarAccesoTracking = verificarAccesoTracking;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            autenticar(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            autorizarSuscripcion(accessor);
        }

        return message;
    }

    private void autenticar(StompHeaderAccessor accessor) {
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

    /**
     * Autoriza (o rechaza) el SUBSCRIBE al topic de tracking de un agente.
     *
     * Solo actúa sobre destinos que matchean el patrón del tracking
     * ("/topic/agente/{agenteId}/ubicacion") — cualquier otro destino
     * pasa sin restricción adicional aquí, esta épica cubre únicamente
     * P6 (tracking), no introduce un modelo de autorización genérico
     * para todos los topics futuros.
     */
    private void autorizarSuscripcion(StompHeaderAccessor accessor) {
        String destino = accessor.getDestination();
        if (destino == null) return;

        Matcher matcher = TOPIC_UBICACION_AGENTE.matcher(destino);
        if (!matcher.matches()) return; // no es un topic de tracking, no aplica esta regla

        String agenteId = matcher.group(1);

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new MessagingException(
                "Suscripcion rechazada: sesion STOMP no autenticada.");
        }

        String actorId = principal.getName();
        String rol = (principal instanceof Authentication auth)
            ? auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("")
            : "";

        if (!verificarAccesoTracking.puedeAcceder(agenteId, actorId, rol)) {
            throw new MessagingException(
                "Suscripcion rechazada: no tiene autorizacion para ver "
                + "la ubicacion de este agente.");
        }
    }
}