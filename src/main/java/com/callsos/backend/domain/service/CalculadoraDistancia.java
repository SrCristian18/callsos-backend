/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.service;

import com.callsos.backend.domain.valueobject.Ubicacion;

/**
 * Épica 4: helper de distancia Haversine, extraído de
 * SimularRecorridoAgenteService (donde vivía duplicado como método
 * privado) para que CalcularEtaService también lo use, sin repetir la
 * fórmula en dos lugares.
 *
 * Vive en domain/service porque es lógica de dominio pura — no depende
 * de infraestructura ni de puertos, solo de value objects (Ubicacion).
 *
 * Utilidad estática y sin estado: no hay razón para instanciarla ni
 * para inyectarla como bean — cualquier caso de uso o servicio de
 * dominio puede llamarla directamente.
 */
public final class CalculadoraDistancia {

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    private CalculadoraDistancia() {
        // utilidad estática — no instanciable
    }

    /**
     * Distancia en línea recta entre dos coordenadas, en metros
     * (fórmula de Haversine — no considera calles ni rutas reales,
     * usada aquí como aproximación suficiente para categorizar
     * distancia y estimar tiempo de llegada).
     */
    public static double distanciaMetros(Ubicacion a, Ubicacion b) {
        double dLat = Math.toRadians(b.getLatitud() - a.getLatitud());
        double dLon = Math.toRadians(b.getLongitud() - a.getLongitud());
        double lat1 = Math.toRadians(a.getLatitud());
        double lat2 = Math.toRadians(b.getLatitud());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.sqrt(h));
    }
}
