/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.infrastructure.adapter.out.persistence.AgenteByidRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — "Tests de repositorios faltantes: AgenteByidRepositoryMySQL".
 *
 * Puerto segregado (ISP) exclusivamente para buscarPorId — separado de
 * AgenteRepositoryPort. Usa el agente semilla ag-test-001 de data-test.sql
 * y, para el caso de ubicación nula, inserta uno propio vía JdbcTemplate.
 */
@JdbcTest
@Import(AgenteByidRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("AgenteByidRepositoryMySQL — integración H2")
class AgenteByidRepositoryMySQLTest {

    @Autowired private AgenteByidRepositoryMySQL repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("buscarPorId retorna el agente semilla de data-test.sql")
    void buscarPorIdEncontrado() {
        Optional<Agente> encontrado = repository.buscarPorId("ag-test-001");

        assertTrue(encontrado.isPresent());
        assertEquals("ag-test-001", encontrado.get().getId());
        assertEquals("Pedro Test", encontrado.get().getNombre());
        assertTrue(encontrado.get().estaDisponible());
    }

    @Test
    @DisplayName("buscarPorId retorna vacío si el agente no existe")
    void buscarPorIdNoEncontrado() {
        Optional<Agente> resultado = repository.buscarPorId("no-existe-xyz");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("buscarPorId reconstituye estado OCUPADO correctamente")
    void buscarPorIdAgenteOcupado() {
        jdbc.update(
            "UPDATE agentes SET estado = 'OCUPADO' WHERE id = 'ag-test-001'");

        Optional<Agente> encontrado = repository.buscarPorId("ag-test-001");

        assertTrue(encontrado.isPresent());
        assertFalse(encontrado.get().estaDisponible());
    }

    @Test
    @DisplayName("buscarPorId retorna ubicación null cuando latitud y longitud son 0")
    void buscarPorIdSinUbicacion() {
        jdbc.update(
            """
            INSERT INTO agentes
                (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
            VALUES ('ag-sin-ubicacion', 'Agente Sin Ubicacion', 'N/A', 0, 0, '3000000000', 'DISPONIBLE', 'cai-test-001')
            """);

        Optional<Agente> encontrado = repository.buscarPorId("ag-sin-ubicacion");

        assertTrue(encontrado.isPresent());
        assertNull(encontrado.get().getUbicacion());
    }
}
