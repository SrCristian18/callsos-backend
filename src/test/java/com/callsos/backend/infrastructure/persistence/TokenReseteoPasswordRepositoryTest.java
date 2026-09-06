/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.model.TokenReseteoPassword;
import com.callsos.backend.infrastructure.adapter.out.persistence.TokenReseteoPasswordRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 8 (hallazgo #6, Parte 2). Mismo patrón que
 * InvitacionAgenteRepositoryMySQLTest — usa el denunciante semilla
 * den-test-001 de data-test.sql como actorId.
 */
@JdbcTest
@Import(TokenReseteoPasswordRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("TokenReseteoPasswordRepositoryMySQL — integración H2")
class TokenReseteoPasswordRepositoryMySQLTest {

    @Autowired
    private TokenReseteoPasswordRepositoryMySQL repository;

    @Test
    @DisplayName("guardar y buscarPorToken — ciclo completo")
    void guardarYBuscarPorToken() {
        TokenReseteoPassword token = TokenReseteoPassword.generar("den-test-001");
        repository.guardar(token);

        Optional<TokenReseteoPassword> encontrado = repository.buscarPorToken(token.getToken());

        assertTrue(encontrado.isPresent());
        assertEquals(token.getToken(), encontrado.get().getToken());
        assertEquals("den-test-001", encontrado.get().getActorId());
        assertFalse(encontrado.get().isUsado());
        assertNull(encontrado.get().getFechaUso());
    }

    @Test
    @DisplayName("buscarPorToken retorna vacío para un token inexistente")
    void buscarPorTokenInexistente() {
        Optional<TokenReseteoPassword> resultado = repository.buscarPorToken("token-que-no-existe");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("actualizar persiste el marcado como usado")
    void actualizarMarcaComoUsado() {
        TokenReseteoPassword token = TokenReseteoPassword.generar("den-test-001");
        repository.guardar(token);

        token.marcarUsado();
        repository.actualizar(token);

        Optional<TokenReseteoPassword> encontrado = repository.buscarPorToken(token.getToken());

        assertTrue(encontrado.isPresent());
        assertTrue(encontrado.get().isUsado());
        assertNotNull(encontrado.get().getFechaUso());
    }

    @Test
    @DisplayName("un token recién generado está vigente")
    void tokenNuevoEstaVigente() {
        TokenReseteoPassword token = TokenReseteoPassword.generar("den-test-001");
        repository.guardar(token);

        Optional<TokenReseteoPassword> encontrado = repository.buscarPorToken(token.getToken());

        assertTrue(encontrado.isPresent());
        assertTrue(encontrado.get().estaVigente());
    }

    @Test
    @DisplayName("un token usado ya no está vigente")
    void tokenUsadoNoEstaVigente() {
        TokenReseteoPassword token = TokenReseteoPassword.generar("den-test-001");
        repository.guardar(token);
        token.marcarUsado();
        repository.actualizar(token);

        Optional<TokenReseteoPassword> encontrado = repository.buscarPorToken(token.getToken());

        assertTrue(encontrado.isPresent());
        assertFalse(encontrado.get().estaVigente());
    }
}