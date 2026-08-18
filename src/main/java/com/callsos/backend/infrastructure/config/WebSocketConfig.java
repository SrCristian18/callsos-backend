/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.infrastructure.config.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
 
/**
 * Configuración de WebSocket con protocolo STOMP.
 *
 * STOMP (Simple Text Oriented Messaging Protocol) es una capa de mensajería
 * sobre WebSocket que define conceptos de destino (topic/queue) y frames
 * (SEND, SUBSCRIBE, MESSAGE). Spring lo implementa de forma nativa.
 *
 * Topología de mensajes para el tracking del agente (Épica 3):
 *
 *   App Flutter (agente)  →  SEND /app/ubicacion/{incidenteId}
 *                         →  UbicacionAgenteController procesa
 *                         →  SimpMessagingTemplate.convertAndSend(
 *                               "/topic/agente/{agenteId}/ubicacion")
 *                         →  Suscritos autorizados: el propio AGENTE,
 *                            OPERADOR_CAI de su unidad, COMANDO — NUNCA
 *                            el DENUNCIANTE (ver StompAuthChannelInterceptor
 *                            y VerificarAccesoTrackingService).
 *
 * Antes de la Épica 3 el topic se nombraba por incidenteId
 * ("/topic/incidente/{id}/ubicacion") y no había ninguna autorización
 * sobre SUBSCRIBE — cualquier cliente autenticado, sin importar el rol,
 * podía suscribirse solo con conocer el incidenteId. Eso permitía al
 * DENUNCIANTE ver la ubicación GPS real del agente, justo lo que la
 * Regla 4 del análisis técnico prohíbe explícitamente.
 *
 * Endpoints:
 *   /ws          → conexión WebSocket con SockJS fallback
 *   /app/**      → mensajes que van a @MessageMapping (cliente → servidor)
 *   /topic/**    → mensajes broadcast (servidor → clientes suscritos)
 *   /queue/**    → mensajes a usuario específico (servidor → un cliente)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    // Registra el interceptor JWT sobre el canal de mensajes ENTRANTES
    // (cliente -> servidor). Se ejecuta en cada frame STOMP, pero el
    // interceptor solo actua sobre CONNECT (ver StompAuthChannelInterceptor).
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker simple en memoria para /topic (broadcast) y /queue (unicast)
        registry.enableSimpleBroker("/topic", "/queue");
 
        // Prefijo para mensajes dirigidos a @MessageMapping en controllers
        registry.setApplicationDestinationPrefixes("/app");
    }
 
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            // SockJS: fallback automático cuando WebSocket no está disponible
            // (redes corporativas, proxies, navegadores antiguos)
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}