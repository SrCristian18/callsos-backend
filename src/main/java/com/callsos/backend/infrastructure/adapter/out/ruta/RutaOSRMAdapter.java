package com.callsos.backend.infrastructure.adapter.out.ruta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.callsos.backend.domain.port.out.RutaPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class RutaOSRMAdapter implements RutaPort{

    private static final Logger log = LoggerFactory.getLogger(RutaOSRMAdapter.class);
    private static final int PUNTOS_FALLBACK = 20;
    private final RestClient restClient;
    private final String osrmBaseUrl;

    public RutaOSRMAdapter(
        @Value("${SIMULACION_OSRM_URL:https://router.project-osrm.org}") String osrmBaseUrl,
        @Value("${SIMULACION_OSRM_TIMEOUT_MS:4000}") int timeoutMs) 
        {
            this.osrmBaseUrl = osrmBaseUrl;

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);

            this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(osrmBaseUrl)
                .build();
        }
    
    @Override
    public List<Ubicacion> calcularRuta(Ubicacion origen, Ubicacion destino) 
    {
    try{
        String path = String.format(Locale.ROOT,
            "/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
            origen.getLongitud(), origen.getLatitud(),
            destino.getLongitud(), destino.getLatitud());
            
        JsonNode respuesta = restClient.get()
            .uri(path)
            .retrieve()
            .body(JsonNode.class);

        List<Ubicacion> ruta = extraerPuntos(respuesta);

        if(ruta.size() < 2)
        {
            log.warn("OSRM devolvió una ruta vacía/incompleta, usando linea recta como respaldo.");
            return lineaRecta(origen, destino);
        }
            
        log.info("Ruta OSRM calculada: {} puntos entre {} y {}", ruta.size(), origen, destino);
        return ruta;

        }catch (Exception e)
        {
            log.warn("No se pudo calcular ruta como OSRM ({}), usando linea recta como resplado",
                e.getMessage());
            return lineaRecta(origen, destino);
        }
    }
        
    private List<Ubicacion> extraerPuntos(JsonNode respuesta)
    {
        List<Ubicacion> puntos = new ArrayList<>();
        if (respuesta == null) return puntos;

        JsonNode routes = respuesta.path("routes");
        if(!routes.isArray() || routes.isEmpty()) return puntos;

        JsonNode coordenadas = routes.get(0).path("geometry").path("coordinates");
        if(!coordenadas.isArray()) return puntos;

        for(JsonNode par : coordenadas)
        {
            double lon = par.get(0).asDouble();
            double lat = par.get(1).asDouble();
            puntos.add(new Ubicacion(lat, lon));
        }
        return puntos;
    }

    private List<Ubicacion> lineaRecta(Ubicacion origen, Ubicacion destino) 
    {
        List<Ubicacion> puntos = new ArrayList<>();
        for (int i = 0; i < PUNTOS_FALLBACK; i++) {
            double t = i / (double) (PUNTOS_FALLBACK - 1);
            double lat = origen.getLatitud() + t * (destino.getLatitud() - origen.getLatitud());
            double lon = origen.getLongitud() + t * (destino.getLongitud() - origen.getLongitud());
            puntos.add(new Ubicacion(lat, lon));
        }
        return puntos;
    }
}