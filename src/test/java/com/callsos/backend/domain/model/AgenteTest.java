/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoAgente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
 
import static org.junit.jupiter.api.Assertions.*;
 
@DisplayName("Agente — hoja del patrón Composite")
class AgenteTest {
     private Agente agente;
 
    @BeforeEach
    void setUp() {
        agente = new Agente("ag-001", "Pedro Ruiz",
            "Av. Principal", new Ubicacion(10.39, -75.51), "3009876543");
    }
 
    @Test @DisplayName("Nace DISPONIBLE")
    void naceDisponible() {
        assertEquals(EstadoAgente.DISPONIBLE, agente.getEstado());
        assertTrue(agente.estaDisponible());
    }
 
    @Test @DisplayName("asignar() pone OCUPADO")
    void asignarPoneOcupado() {
        agente.asignar();
        assertEquals(EstadoAgente.OCUPADO, agente.getEstado());
        assertFalse(agente.estaDisponible());
    }
 
    @Test @DisplayName("liberar() devuelve a DISPONIBLE")
    void liberarDevuelveDisponible() {
        agente.asignar();
        agente.liberar();
        assertEquals(EstadoAgente.DISPONIBLE, agente.getEstado());
    }
 
    @Test @DisplayName("No se puede asignar si ya está OCUPADO")
    void noAsignarSiOcupado() {
        agente.asignar();
        assertThrows(IllegalStateException.class, () -> agente.asignar());
    }
 
    @Test @DisplayName("Hoja Composite rechaza agregar subordinados")
    void hojaRechazaSubordinados() {
        Agente otro = new Agente("ag-002", "Luis",
            "Calle 2", new Ubicacion(10.40, -75.52), "3001111111");
        assertThrows(UnsupportedOperationException.class,
            () -> agente.agregar(otro));
    }
}
