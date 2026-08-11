package com.callsos.backend.domain.port.in;

public interface SimularRecorridoAgentePort {
    void iniciar(String incidenteId);
    void detener(String incidenteId);
}
