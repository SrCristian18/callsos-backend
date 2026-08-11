package com.callsos.backend.domain.port.in;

import com.callsos.backend.domain.valueobject.Ubicacion;

public interface PublicarUbicacionAgentePort {
    void publicar(String agenteId, String incidenteId, Ubicacion ubicacion);
}
