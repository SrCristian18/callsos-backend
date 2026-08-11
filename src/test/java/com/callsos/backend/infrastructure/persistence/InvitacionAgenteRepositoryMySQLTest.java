/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.infrastructure.adapter.out.persistence.InvitacionAgenteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — GAP DETECTADO, no listado en la ruta original:
 * InvitacionAgenteRepositoryMySQL tampoco tenía test, aunque la Épica 4
 * solo mencionaba 6 repositorios faltantes. Se agrega aquí junto con los
 * demás para no dejar el gap sin cerrar.
 *
 * Usa la unidad semilla cai-test-001 de data-test.sql como unidad_policial_id.
 */
@JdbcTest
@Import(InvitacionAgenteRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("InvitacionAgenteRepositoryMySQL — integración H2")
class InvitacionAgenteRepositoryMySQLTest {

    @Autowired
    private InvitacionAgenteRepositoryMySQL repository;

    @Test
    @DisplayName("guardar y buscarPorToken — ciclo completo")
    void guardarYBuscarPorToken() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-test-001", "usr-comando-001");
        repository.guardar(invitacion);

        Optional<InvitacionAgente> encontrada = repository.buscarPorToken(invitacion.getToken());

        assertTrue(encontrada.isPresent());
        assertEquals(invitacion.getToken(), encontrada.get().getToken());
        assertEquals("cai-test-001", encontrada.get().getUnidadPolicialId());
        assertEquals("usr-comando-001", encontrada.get().getCreadoPor());
        assertFalse(encontrada.get().isUsado());
        assertNull(encontrada.get().getUsadoPor());
        assertNull(encontrada.get().getFechaUso());
    }

    @Test
    @DisplayName("buscarPorToken retorna vacío para un token inexistente")
    void buscarPorTokenInexistente() {
        Optional<InvitacionAgente> resultado = repository.buscarPorToken("token-que-no-existe");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("actualizar persiste el marcado como usado")
    void actualizarMarcaComoUsado() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-test-001", "usr-comando-001");
        repository.guardar(invitacion);

        invitacion.marcarUsado("ag-nuevo-001");
        repository.actualizar(invitacion);

        Optional<InvitacionAgente> encontrada = repository.buscarPorToken(invitacion.getToken());

        assertTrue(encontrada.isPresent());
        assertTrue(encontrada.get().isUsado());
        assertEquals("ag-nuevo-001", encontrada.get().getUsadoPor());
        assertNotNull(encontrada.get().getFechaUso());
    }

    @Test
    @DisplayName("una invitación recién generada está vigente")
    void invitacionNuevaEstaVigente() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-test-001", "usr-comando-001");
        repository.guardar(invitacion);

        Optional<InvitacionAgente> encontrada = repository.buscarPorToken(invitacion.getToken());

        assertTrue(encontrada.isPresent());
        assertTrue(encontrada.get().estaVigente());
    }

    @Test
    @DisplayName("una invitación usada ya no está vigente")
    void invitacionUsadaNoEstaVigente() {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-test-001", "usr-comando-001");
        repository.guardar(invitacion);
        invitacion.marcarUsado("ag-nuevo-001");
        repository.actualizar(invitacion);

        Optional<InvitacionAgente> encontrada = repository.buscarPorToken(invitacion.getToken());

        assertTrue(encontrada.isPresent());
        assertFalse(encontrada.get().estaVigente());
    }
}
