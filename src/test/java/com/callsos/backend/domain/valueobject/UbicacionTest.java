/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.valueobject;

/**
 *
 * @author LENOVO
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
 
import static org.junit.jupiter.api.Assertions.*;
 
@DisplayName("Ubicacion — value object")
class UbicacionTest {
     @Test
    @DisplayName("Crea correctamente con coordenadas válidas")
    void creaConCoordenadasValidas() {
        Ubicacion u = new Ubicacion(10.39, -75.51);
        assertEquals(10.39, u.getLatitud());
        assertEquals(-75.51, u.getLongitud());
    }
 
    @ParameterizedTest(name = "lat={0} lon={1}")
    @CsvSource({"-91, 0", "91, 0", "0, -181", "0, 181"})
    @DisplayName("Rechaza coordenadas fuera de rango")
    void rechazaCoordenadasFueraDeRango(double lat, double lon) {
        assertThrows(IllegalArgumentException.class,
            () -> new Ubicacion(lat, lon));
    }
 
    @Test
    @DisplayName("Igualdad por valor, no por referencia")
    void igualdadPorValor() {
        Ubicacion a = new Ubicacion(10.39, -75.51);
        Ubicacion b = new Ubicacion(10.39, -75.51);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
 
    @Test
    @DisplayName("actualizarUbicacion retorna nueva instancia inmutable")
    void actualizarRetornaNuevaInstancia() {
        Ubicacion original = new Ubicacion(10.0, -75.0);
        Ubicacion nueva    = new Ubicacion(11.0, -76.0);
        Ubicacion resultado = original.actualizarUbicacion(nueva);
        assertEquals(11.0, resultado.getLatitud());
        assertNotSame(original, resultado);
    }
}
