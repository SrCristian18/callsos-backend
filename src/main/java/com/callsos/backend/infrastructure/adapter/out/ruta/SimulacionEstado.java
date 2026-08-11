package com.callsos.backend.infrastructure.adapter.out.ruta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.stereotype.Component;

@Component
public class SimulacionEstado {
    private final Map<String, ScheduledFuture<?>> simulacionesActivas = new ConcurrentHashMap<>();

    public boolean estaSimulando(String incidenteId){
        return simulacionesActivas.containsKey(incidenteId);
    }

    public void registrar(String incidenteId, ScheduledFuture<?> tarea){
        simulacionesActivas.put(incidenteId, tarea);
    }

    public void detener(String incidenteId){
        ScheduledFuture<?> tarea = simulacionesActivas.remove(incidenteId);
        if(tarea != null){
            tarea.cancel(false);
        }
    }
}