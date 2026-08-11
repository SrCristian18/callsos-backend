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
 
        messagingTemplate.convertAndSend(
            "/topic/incidente/" + incidenteId + "/ubicacion",
            new UbicacionPublicada(
                ubicacion.getLatitud(),
                ubicacion.getLongitud(),
                ua.getTimestamp().toString()
            )
        );
    }
    public record UbicacionPublicada(double latitud, double longitud, String timestamp){}
}
