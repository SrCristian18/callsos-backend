/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.IncidenteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Test de integración: adaptador JDBC contra H2 en memoria.
 *
 * @JdbcTest levanta solo el contexto JDBC (sin web, sin seguridad).
 * @ActiveProfiles("test") usa application-test.yml con H2.
 * Los datos iniciales vienen de data-test.sql.
 */
@JdbcTest
@Import(IncidenteRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("IncidenteRepositoryMySQL — integración H2")
class IncidenteRepositoryMySQLTest {
    
      @Autowired
    private IncidenteRepositoryMySQL repository;
 
    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "den-test-001", "Juan Test", "Cartagena",
        "3001111111", "juan@test.com");
 
    @Test
    @DisplayName("guardar y buscarPorId — ciclo completo")
    void guardarYBuscar() {
        Incidente incidente = new Incidente(
            "i-integ-001", TipoIncidente.ROBOS_O_ASALTOS,
            "Robo en zona portuaria", ubicacion, denunciante);
 
        repository.guardar(incidente);
 
        Optional<Incidente> encontrado = repository.buscarPorId("i-integ-001");
        assertTrue(encontrado.isPresent());
        assertEquals("i-integ-001", encontrado.get().getId());
        assertEquals(TipoIncidente.ROBOS_O_ASALTOS, encontrado.get().getTipo());
        assertEquals(EstadoIncidente.CREADO, encontrado.get().getEstado());
    }
 
    @Test
    @DisplayName("guardar persiste la unidad policial asignada")
    void guardaUnidadPolicial() {
        Incidente incidente = new Incidente(
            "i-integ-002", TipoIncidente.RIÑAS_O_PELEAS,
            "Riña en parque", ubicacion, denunciante);
 
        UnidadPolicial cai = new UnidadPolicial(
            "cai-test-001", "CAI Test Manga",
            "Calle Test 1", ubicacion, "6010000");
        incidente.derivarACAI(cai);
 
        repository.guardar(incidente);
 
        Optional<Incidente> encontrado = repository.buscarPorId("i-integ-002");
        assertTrue(encontrado.isPresent());
        assertNotNull(encontrado.get().getUnidadPolicial());
        assertEquals("cai-test-001", encontrado.get().getUnidadPolicial().getId());
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, encontrado.get().getEstado());
    }
 
    @Test
    @DisplayName("actualizarEstado cambia el estado en BD")
    void actualizarEstado() {
        Incidente incidente = new Incidente(
            "i-integ-003", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante);
        repository.guardar(incidente);
 
        repository.actualizarEstado("i-integ-003", EstadoIncidente.DERIVADO_A_CAI);
 
        Optional<Incidente> actualizado = repository.buscarPorId("i-integ-003");
        assertTrue(actualizado.isPresent());
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, actualizado.get().getEstado());
    }
 
    @Test
    @DisplayName("buscarPorId retorna vacío si el incidente no existe")
    void buscarInexistente() {
        Optional<Incidente> resultado = repository.buscarPorId("no-existe-xyz");
        assertTrue(resultado.isEmpty());
    }
}
