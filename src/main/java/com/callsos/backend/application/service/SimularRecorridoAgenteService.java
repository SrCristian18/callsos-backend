package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.PublicarUbicacionAgentePort;
import com.callsos.backend.domain.port.in.SimularRecorridoAgentePort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.RutaPort;
import com.callsos.backend.domain.service.CalculadoraDistancia;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.ruta.SimulacionEstado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
 
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SimularRecorridoAgenteService implements SimularRecorridoAgentePort{

    private static final Logger log = LoggerFactory.getLogger(SimularRecorridoAgenteService.class);

    private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
    private final RutaPort rutaPort;
    private final PublicarUbicacionAgentePort publicarUbicacion;
    private final SimulacionEstado simulacionEstado;
    private final TaskScheduler taskScheduler;
    private final double velocidadKmh;
    private final long intervaloMs;


    public SimularRecorridoAgenteService(IncidenteRepositoryPort incidenteRepository,
            AsignacionRepositoryPort asignacionRepository, RutaPort rutaPort,
            PublicarUbicacionAgentePort publicarUbicacion, SimulacionEstado simulacionEstado,
            TaskScheduler taskScheduler, double velocidadKmh, long intervaloMs) {
        this.incidenteRepository = incidenteRepository;
        this.asignacionRepository = asignacionRepository;
        this.rutaPort = rutaPort;
        this.publicarUbicacion = publicarUbicacion;
        this.simulacionEstado = simulacionEstado;
        this.taskScheduler = taskScheduler;
        this.velocidadKmh = velocidadKmh;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void iniciar(String incidenteId) {
            if (simulacionEstado.estaSimulando(incidenteId)) {
            log.info("Ya hay una simulación activa para el incidente {}, se ignora.", incidenteId);
            return;
        }
 
        Incidente incidente = incidenteRepository.buscarPorId(incidenteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Incidente no encontrado: " + incidenteId));
 
        String agenteId = asignacionRepository.buscarPorIncidente(incidenteId)
            .map(a -> a.getAgente().getId())
            .orElseThrow(() -> new IllegalStateException(
                "No hay agente asignado — no se puede simular el recorrido."));
 
        if (incidente.getUnidadPolicial() == null) {
            throw new IllegalStateException(
                "El incidente no tiene CAI asignado — no hay punto de origen para simular.");
        }
 
        Ubicacion origen  = incidente.getUnidadPolicial().getUbicacion();
        Ubicacion destino = incidente.getUbicacion();
 
        List<Ubicacion> ruta = rutaPort.calcularRuta(origen, destino);
        double distanciaTotalM = distanciaTotal(ruta);
 
        double velocidadMs = (velocidadKmh * 1000) / 3600.0;
        long duracionTotalMs = (long) ((distanciaTotalM / velocidadMs) * 1000);
        int numTicks = Math.max(2, (int) (duracionTotalMs / intervaloMs));
 
        List<Ubicacion> puntosSimulados = remuestrear(ruta, numTicks);
 
        log.info("Simulación iniciada — incidente {} | agente {} | {} puntos | ~{} s",
            incidenteId, agenteId, puntosSimulados.size(), duracionTotalMs / 1000);
 
        AtomicInteger indice = new AtomicInteger(0);
 
        Runnable tick = () -> {
            int i = indice.getAndIncrement();
            if (i >= puntosSimulados.size()) {
                log.info("Simulación finalizada — incidente {} llegó al destino.", incidenteId);
                simulacionEstado.detener(incidenteId);
                return;
            }
            publicarUbicacion.publicar(agenteId, incidenteId, puntosSimulados.get(i));
        };
 
        ScheduledFuture<?> tarea = taskScheduler.scheduleAtFixedRate(
            tick, Instant.now(), Duration.ofMillis(intervaloMs));
 
        simulacionEstado.registrar(incidenteId, tarea);
    }

    @Override
    public void detener(String incidenteId) {
            simulacionEstado.detener(incidenteId);
        log.info("Simulación detenida manualmente — incidente {}.", incidenteId);
    }

//metodos de apoyo
    private double distanciaTotal(List<Ubicacion> ruta) {
        double total = 0;
        for (int i = 1; i < ruta.size(); i++) {
            total += CalculadoraDistancia.distanciaMetros(ruta.get(i - 1), ruta.get(i));
        }
        return total;
    }

    private List<Ubicacion> remuestrear(List<Ubicacion> ruta, int numPuntos) {
        if (ruta.size() < 2 || numPuntos < 2) return ruta;
 
        double[] acumulado = new double[ruta.size()];
        for (int i = 1; i < ruta.size(); i++) {
            acumulado[i] = acumulado[i - 1] + CalculadoraDistancia.distanciaMetros(ruta.get(i - 1), ruta.get(i));
        }
        double distanciaTotal = acumulado[ruta.size() - 1];
 
        List<Ubicacion> resultado = new ArrayList<>(numPuntos);
        for (int p = 0; p < numPuntos; p++) {
            double objetivo = distanciaTotal * p / (numPuntos - 1);
 
            int idx = 0;
            while (idx < acumulado.length - 2 && acumulado[idx + 1] < objetivo) idx++;
 
            double segInicio = acumulado[idx];
            double segFin = acumulado[Math.min(idx + 1, acumulado.length - 1)];
            double t = segFin > segInicio ? (objetivo - segInicio) / (segFin - segInicio) : 0;
 
            Ubicacion a = ruta.get(idx);
            Ubicacion b = ruta.get(Math.min(idx + 1, ruta.size() - 1));
 
            double lat = a.getLatitud() + t * (b.getLatitud() - a.getLatitud());
            double lon = a.getLongitud() + t * (b.getLongitud() - a.getLongitud());
            resultado.add(new Ubicacion(lat, lon));
        }
        return resultado;
    }
}