/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.CategoriaDistancia;

/**
 * Épica 4: resultado del cálculo de ETA expuesto al denunciante.
 *
 * Deliberadamente NO lleva lat/lon ni distancia exacta en metros/km —
 * solo minutosEstimados (redondeado) y categoriaDistancia (bucketizada).
 * Esto no es una omisión de mapeo: es la garantía de seguridad de esta
 * épica. Cualquier adaptador (REST, WS) que serialice este objeto no
 * puede filtrar coordenadas por accidente porque el objeto mismo no
 * las contiene.
 *
 * minutosEstimados/categoriaDistancia son ambos null cuando no hay
 * datos suficientes para calcular un ETA (agente aún no reportó
 * posición, o el incidente no está en AGENTE_EN_CAMINO) — sinDatos()
 * representa ese caso explícitamente en vez de forzar un 0 o un valor
 * centinela que el cliente pueda malinterpretar como "está muy cerca".
 *
 * Las fábricas estáticas son lógica de dominio pura (distancia +
 * velocidad → tiempo, y bucketización de distancia) — no dependen de
 * ningún puerto, por lo que tanto CalcularEtaService (caso de uso
 * expuesto por REST, con ownership) como PublicarUbicacionAgenteService
 * (broadcast automático por WS en cada actualización GPS) pueden
 * reutilizarlas sin duplicar la fórmula ni acoplarse entre sí.
 */
public class EtaInfo {

    private final Integer minutosEstimados;
    private final CategoriaDistancia categoriaDistancia;

    private EtaInfo(Integer minutosEstimados, CategoriaDistancia categoriaDistancia) {
        this.minutosEstimados  = minutosEstimados;
        this.categoriaDistancia = categoriaDistancia;
    }

    /**
     * Calcula el ETA a partir de una distancia (metros) y una velocidad
     * media configurada (km/h). Los minutos se redondean hacia arriba
     * (Math.ceil) — es preferible sobreestimar levemente el tiempo de
     * llegada de una patrulla que subestimarlo.
     */
    public static EtaInfo calcular(double distanciaMetros, double velocidadMediaKmh) {
        double velocidadMs = (velocidadMediaKmh * 1000) / 3600.0;
        int minutos = (int) Math.ceil((distanciaMetros / velocidadMs) / 60.0);
        return new EtaInfo(minutos, CategoriaDistancia.desde(distanciaMetros));
    }

    /** No hay datos suficientes (sin posición conocida del agente, o el agente aún no salió). */
    public static EtaInfo sinDatos() {
        return new EtaInfo(null, null);
    }

    public Integer getMinutosEstimados()            { return minutosEstimados; }
    public CategoriaDistancia getCategoriaDistancia() { return categoriaDistancia; }

    public boolean tieneDatos() {
        return minutosEstimados != null;
    }
}
