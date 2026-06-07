/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.UbicacionAgenteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
 
import java.util.List;
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Test de integración: UbicacionAgenteRepositoryMySQL contra H2.
 * Verifica guardar, buscarPorIncidente y ultimaPosicion.
 * Es el repositorio usado por el tracking en tiempo real via WebSocket.
 */
@JdbcTest
@Import(UbicacionAgenteRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("UbicacionAgenteRepositoryMySQL — integración H2")
public class UbicacionAgenteRepositoryMySQLTest {
    
    @Autowired
    private UbicacionAgenteRepositoryMySQL repository;
 
    @Test
    @DisplayName("guardar persiste la posición GPS correctamente")
    void guardarPersistePosicion() {
        UbicacionAgente ua = new UbicacionAgente(
            "ag-test-001", "i-test-001",
            new Ubicacion(10.41, -75.54));
 
        repository.guardar(ua);
 
        List<UbicacionAgente> lista =
            repository.buscarPorIncidente("i-test-001");
 
        assertFalse(lista.isEmpty());
        UbicacionAgente guardada = lista.get(0);
        assertEquals("ag-test-001", guardada.getAgenteId());
        assertEquals(10.41, guardada.getUbicacion().getLatitud(), 0.001);
        assertEquals(-75.54, guardada.getUbicacion().getLongitud(), 0.001);
    }
 
    @Test
    @DisplayName("buscarPorIncidente retorna posiciones en orden cronologico")
    void buscarPorIncidenteOrdenado() {
        repository.guardar(new UbicacionAgente(
            "ag-test-001", "i-orden-001", new Ubicacion(10.40, -75.50)));
        repository.guardar(new UbicacionAgente(
            "ag-test-001", "i-orden-001", new Ubicacion(10.41, -75.51)));
        repository.guardar(new UbicacionAgente(
            "ag-test-001", "i-orden-001", new Ubicacion(10.42, -75.52)));
 
        List<UbicacionAgente> lista =
            repository.buscarPorIncidente("i-orden-001");
 
        assertEquals(3, lista.size());
        // Primera posición guardada debe ser la primera en la lista (ASC)
        assertEquals(10.40, lista.get(0).getUbicacion().getLatitud(), 0.001);
        assertEquals(10.42, lista.get(2).getUbicacion().getLatitud(), 0.001);
    }
 
    @Test
    @DisplayName("ultimaPosicion retorna la posicion mas reciente del agente")
    void ultimaPosicionMasReciente() {
        repository.guardar(new UbicacionAgente(
            "ag-test-001", "i-ultima-001", new Ubicacion(10.40, -75.50)));
        repository.guardar(new UbicacionAgente(
            "ag-test-001", "i-ultima-001", new Ubicacion(10.45, -75.55)));
 
        Optional<UbicacionAgente> ultima =
            repository.ultimaPosicion("ag-test-001", "i-ultima-001");
 
        assertTrue(ultima.isPresent());
        // La ultima guardada (10.45) debe ser la recuperada
        assertEquals(10.45, ultima.get().getUbicacion().getLatitud(), 0.001);
    }
 
    @Test
    @DisplayName("buscarPorIncidente retorna vacio si no hay posiciones")
    void buscarVacioSiNoHayPosiciones() {
        List<UbicacionAgente> lista =
            repository.buscarPorIncidente("incidente-sin-tracking");
        assertTrue(lista.isEmpty());
    }
 
    @Test
    @DisplayName("ultimaPosicion retorna vacio si no hay posiciones del agente")
    void ultimaPosicionVaciaSiNoExiste() {
        Optional<UbicacionAgente> ultima =
            repository.ultimaPosicion("agente-sin-pos", "incidente-x");
        assertTrue(ultima.isEmpty());
    }
}
