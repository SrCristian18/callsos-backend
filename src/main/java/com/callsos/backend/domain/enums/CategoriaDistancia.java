/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.callsos.backend.domain.enums;

/**
 *
 * @author LENOVO
 */

/**
 * Épica 4: distancia del agente al incidente, categorizada en rangos
 * en vez de exponerse como metros/km exactos ni, mucho menos, como
 * coordenadas — el criterio de aceptación es explícito: "el denunciante
 * recibe minutos y distancia categorizada, nunca coordenadas".
 *
 * Los umbrales son fijos por ahora (no hay una necesidad expresada de
 * configurarlos por entorno, a diferencia de eta.velocidad-media-kmh).
 */
public enum CategoriaDistancia {
    MENOS_DE_1_KM,
    ENTRE_1_Y_3_KM,
    ENTRE_3_Y_10_KM,
    MAS_DE_10_KM;

    public static CategoriaDistancia desde(double metros) {
        double km = metros / 1000.0;
        if (km < 1)  return MENOS_DE_1_KM;
        if (km < 3)  return ENTRE_1_Y_3_KM;
        if (km < 10) return ENTRE_3_Y_10_KM;
        return MAS_DE_10_KM;
    }
}
