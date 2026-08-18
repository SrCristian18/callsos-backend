package com.callsos.backend.application.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.EtaInfo;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.in.PublicarUbicacionAgentePort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.service.CalculadoraDistancia;
import com.callsos.backend.domain.valueobject.Ubicacion;

public class PublicarUbicacionAgenteService implements PublicarUbicacionAgentePort{

    private final UbicacionAgenteRepositoryPort repositorio;
    private final SimpMessagingTemplate messagingTemplate;
    private final IncidenteRepositoryPort incidenteRepository;
    private final double velocidadMediaKmh;

    public PublicarUbicacionAgenteService(UbicacionAgenteRepositoryPort repositorio,
        SimpMessagingTemplate messagingTemplate,
        IncidenteRepositoryPort incidenteRepository,
        double velocidadMediaKmh)
        {
            this.repositorio = repositorio;
            this.messagingTemplate = messagingTemplate;
            this.incidenteRepository = incidenteRepository;
            this.velocidadMediaKmh = velocidadMediaKmh;
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

        publicarEtaSiCorresponde(incidenteId, ubicacion);
    }

    /**
     * Épica 4: cada posición GPS que llega del agente es también la señal
     * natural para recalcular y re-emitir el ETA del denunciante — mismo
     * punto del ciclo de vida donde ya se sabe "hay una posición nueva",
     * sin necesidad de un scheduler aparte ni de que el denunciante
     * solicite el valor por REST (ConsultarEtaPort/CalcularEtaService
     * cubre ese caso de reconexión, ver IncidenteController.eta()).
     *
     * El topic /topic/incidente/{id}/eta se nombra por incidente, no por
     * agente — a diferencia de la ubicación cruda, el ETA en sí NO es
     * información sensible (no permite reconstruir la posición exacta
     * del agente, solo una categoría de distancia + minutos), así que no
     * necesita pasar por StompAuthChannelInterceptor ni por la matriz de
     * VerificarAccesoTrackingPort — cualquier cliente ya autenticado que
     * conozca el incidenteId puede suscribirse (igual que ya ocurre con
     * /topic/incidente/{id}/actualizaciones y otros topics informativos).
     *
     * No se transmite (ni se calcula) ETA si el incidente ya no está
     * AGENTE_EN_CAMINO (llegó, se finalizó, se canceló) — evita publicar
     * un valor obsoleto o sin sentido después de ese punto.
     */
    private void publicarEtaSiCorresponde(String incidenteId, Ubicacion posicionAgente) {
        incidenteRepository.buscarPorId(incidenteId).ifPresent(incidente -> {
            if (incidente.getEstado() != EstadoIncidente.AGENTE_EN_CAMINO) {
                return;
            }

            double distanciaMetros = CalculadoraDistancia.distanciaMetros(
                posicionAgente, incidente.getUbicacion());

            EtaInfo eta = EtaInfo.calcular(distanciaMetros, velocidadMediaKmh);

            messagingTemplate.convertAndSend(
                "/topic/incidente/" + incidenteId + "/eta",
                new EtaPublicada(eta.getMinutosEstimados(), eta.getCategoriaDistancia())
            );
        });
    }

    public record UbicacionPublicada(double latitud, double longitud, String timestamp){}

    /**
     * Payload público del ETA — deliberadamente sin lat/lon (ver
     * EtaInfo). "categoriaDistancia" serializa como el nombre del enum
     * (ej. "MENOS_DE_1_KM").
     */
    public record EtaPublicada(
        Integer minutosEstimados,
        com.callsos.backend.domain.enums.CategoriaDistancia categoriaDistancia
    ){}
}