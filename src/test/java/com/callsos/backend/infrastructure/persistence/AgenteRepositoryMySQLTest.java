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
}
