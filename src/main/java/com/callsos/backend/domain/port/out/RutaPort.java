package com.callsos.backend.domain.port.out;

import java.util.List;

import com.callsos.backend.domain.valueobject.Ubicacion;

public interface RutaPort {
    List<Ubicacion> calcularRuta(Ubicacion origen, Ubicacion destino);
}
