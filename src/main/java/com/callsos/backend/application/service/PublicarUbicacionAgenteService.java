package com.callsos.backend.application.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.in.PublicarUbicacionAgentePort;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;

public class PublicarUbicacionAgenteService implements PublicarUbicacionAgentePort{

    private final UbicacionAgenteRepositoryPort repositorio;
    private final SimpMessagingTemplate messagingTemplate;

    public PublicarUbicacionAgenteService(UbicacionAgenteRepositoryPort repositorio,
        SimpMessagingTemplate messagingTemplate)
        {
            this.repositorio = repositorio;
            this.messagingTemplate = messagingTemplate;
        }

    @Override
    public void publicar(String agenteId, String incidenteId, Ubicacion ubicacion) {
        UbicacionAgente ua = new UbicacionAgente(agenteId, incidenteId, ubicacion);
 
        repositorio.guardar(ua);
 
        // Épica 3 (fix P6 — la regla de seguridad más crítica del sistema):
        // ANTES se publicaba a "/topic/incidente/{incidenteId}/ubicacion".
        // Ese topic estaba pensado como "el canal del incidente", pero en
        // la práctica cualquier cliente autenticado (sin importar el rol)
        // podía suscribirse a él manualmente conociendo solo el
        // incidenteId — incluido el propio DENUNCIANTE, exactamente lo que
        // la Regla 4 del análisis técnico prohíbe explícitamente: el
        // denunciante NUNCA puede ver la ubicación GPS del agente.
        //
        // AHORA se publica a "/topic/agente/{agenteId}/ubicacion" — un
        // topic por AGENTE, no por incidente. La autorización real de
        // quién puede suscribirse (el propio agente, un CAI dueño de ese
        // agente, o COMANDO — nunca el denunciante) se aplica en
        // StompAuthChannelInterceptor sobre el comando SUBSCRIBE, que es
        // donde debe vivir: "no basta con que Flutter no muestre el mapa,
        // el backend debe impedir la suscripción no autorizada".
        messagingTemplate.convertAndSend(
            "/topic/agente/" + agenteId + "/ubicacion",
            new UbicacionPublicada(
                ubicacion.getLatitud(),
                ubicacion.getLongitud(),
                ua.getTimestamp().toString()
            )
        );
    }
    public record UbicacionPublicada(double latitud, double longitud, String timestamp){}
}