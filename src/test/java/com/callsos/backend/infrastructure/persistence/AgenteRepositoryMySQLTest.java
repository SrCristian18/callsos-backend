/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.infrastructure.adapter.out.persistence.AgenteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
 
import java.util.List;
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
 
@JdbcTest
@Import(AgenteRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("AgenteRepositoryMySQL — integración H2")
class AgenteRepositoryMySQLTest {
    
    @Autowired
    private AgenteRepositoryMySQL repository;
 
    @Test
    @DisplayName("obtenerDisponibles retorna agentes DISPONIBLE del data-test.sql")
    void obtenerDisponibles() {
        List<Agente> disponibles = repository.obtenerDisponibles();
        assertFalse(disponibles.isEmpty());
        disponibles.forEach(a -> assertTrue(a.estaDisponible()));
    }
 
    @Test
    @DisplayName("obtenerDisponiblesPorUnidad filtra correctamente por unidad")
    void disponiblesPorUnidad() {
        List<Agente> resultado = repository
            .obtenerDisponiblesPorUnidad("cai-test-001");
        assertFalse(resultado.isEmpty());
        assertEquals("ag-test-001", resultado.get(0).getId());
    }
 
    @Test
    @DisplayName("obtenerDisponiblesPorUnidad retorna vacío para unidad inexistente")
    void vistaPorUnidadInexistente() {
        List<Agente> resultado = repository
            .obtenerDisponiblesPorUnidad("unidad-no-existe");
        assertTrue(resultado.isEmpty());
    }
 
    @Test
    @DisplayName("actualizarEstado persiste OCUPADO en BD")
    void actualizarEstado() {
        List<Agente> disponibles = repository.obtenerDisponibles();
        assertFalse(disponibles.isEmpty());
 
        Agente agente = disponibles.get(0);
        agente.asignar(); // DISPONIBLE → OCUPADO en memoria
        repository.actualizarEstado(agente);
 
        // Ya no debe aparecer en disponibles
        List<Agente> tras = repository.obtenerDisponibles();
        boolean sigueDisponible = tras.stream()
            .anyMatch(a -> a.getId().equals(agente.getId()));
        assertFalse(sigueDisponible);
    }

    // ── Épica 3 (fix P6) ─────────────────────────────────────────────────

    @Test
    @DisplayName("buscarUnidadDeAgente retorna la unidad policial del agente")
    void buscarUnidadDeAgenteExistente() {
        Optional<String> unidad = repository.buscarUnidadDeAgente("ag-test-001");

        assertTrue(unidad.isPresent());
        assertEquals("cai-test-001", unidad.get());
    }

    @Test
    @DisplayName("buscarUnidadDeAgente retorna vacío si el agente no existe")
    void buscarUnidadDeAgenteInexistente() {
        Optional<String> unidad = repository.buscarUnidadDeAgente("ag-no-existe");

        assertTrue(unidad.isEmpty());
    }

    // ── Épica 8 (hallazgo #6, Parte 1) ──────────────────────────────────

    @Test
    @DisplayName("guardar() persiste el correo y se recupera correctamente al releer de BD")
    void guardarPersisteCorreo() {
        Agente nuevo = new Agente(
            "ag-test-correo-001", "Nuevo Agente", "Calle Nueva 1",
            null, "3005551111");
        nuevo.setCorreo("nuevo.agente@callsos.test");

        repository.guardar(nuevo, "cai-test-001");

        List<Agente> disponibles = repository.obtenerDisponiblesPorUnidad("cai-test-001");
        Agente releido = disponibles.stream()
            .filter(a -> a.getId().equals("ag-test-correo-001"))
            .findFirst()
            .orElseThrow();

        assertEquals("nuevo.agente@callsos.test", releido.getCorreo());
    }

    @Test
    @DisplayName("agente sembrado sin correo (data-test.sql, previo a este fix) se lee como null sin lanzar")
    void agenteSinCorreoSeLeeComoNull() {
        List<Agente> disponibles = repository.obtenerDisponiblesPorUnidad("cai-test-001");
        Agente sembrado = disponibles.stream()
            .filter(a -> a.getId().equals("ag-test-001"))
            .findFirst()
            .orElseThrow();

        assertNull(sembrado.getCorreo());
    }

    @Test
    @DisplayName("buscarPorCorreo encuentra un agente recién guardado por su correo")
    void buscarPorCorreoEncuentra() {
        Agente nuevo = new Agente(
            "ag-test-correo-002", "Otro Agente", "Calle X",
            null, "3005552222");
        nuevo.setCorreo("otro.agente@callsos.test");
        repository.guardar(nuevo, "cai-test-001");

        Optional<Agente> encontrado = repository.buscarPorCorreo("otro.agente@callsos.test");

        assertTrue(encontrado.isPresent());
        assertEquals("ag-test-correo-002", encontrado.get().getId());
    }

    @Test
    @DisplayName("buscarPorCorreo retorna vacío si ningún agente tiene ese correo")
    void buscarPorCorreoInexistente() {
        assertTrue(repository.buscarPorCorreo("no-existe@callsos.test").isEmpty());
    }
}