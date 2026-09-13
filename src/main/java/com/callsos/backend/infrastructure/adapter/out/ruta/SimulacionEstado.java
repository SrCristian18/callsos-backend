package com.callsos.backend.infrastructure.adapter.out.ruta;

import com.callsos.backend.domain.port.out.SimulacionEstadoPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.stereotype.Component;

/**
 * FIX (auditoría AUD-arquitectura): implementa {@link SimulacionEstadoPort}
 * — la capa de aplicación (SimularRecorridoAgenteService) ahora depende de
 * ese puerto, nunca de esta clase concreta.
 */
@Component
public class SimulacionEstado implements SimulacionEstadoPort {
    private final Map<String, ScheduledFuture<?>> simulacionesActivas = new ConcurrentHashMap<>();

    @Override
    public boolean estaSimulando(String incidenteId){
        return simulacionesActivas.containsKey(incidenteId);
    }

    @Override
    public void registrar(String incidenteId, ScheduledFuture<?> tarea){
        simulacionesActivas.put(incidenteId, tarea);
    }

    @Override
    public void detener(String incidenteId){
        ScheduledFuture<?> tarea = simulacionesActivas.remove(incidenteId);
        if(tarea != null){
            tarea.cancel(false);
        }
    }
}