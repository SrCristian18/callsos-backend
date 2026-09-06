/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.infrastructure.adapter.out.persistence.DenuncianteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Test de integración: DenuncianteRepositoryMySQL contra H2.
 * Verifica buscarPorId (con token_fcm) y actualizarTokenFcm.
 */
@JdbcTest
@Import(DenuncianteRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("DenuncianteRepositoryMySQL — integración H2")
public class DenuncianteRepositoryMySQLTest {
    
    @Autowired
    private DenuncianteRepositoryMySQL repository;
 
    @Test
    @DisplayName("buscarPorId reconstruye el Denunciante con todos sus campos")
    void buscarPorIdCompleto() {
        Optional<Denunciante> resultado =
            repository.buscarPorId("den-test-001");
 
        assertTrue(resultado.isPresent());
        Denunciante d = resultado.get();
        assertEquals("den-test-001",  d.getId());
        assertEquals("Juan Test",     d.getNombre());
        assertEquals("juan@test.com", d.getCorreo());
        // token_fcm es NULL en data-test.sql
        assertNull(d.getTokenFcm());
        assertFalse(d.tieneTokenFcm());
    }
 
    @Test
    @DisplayName("buscarPorId retorna vacío si no existe")
    void buscarInexistente() {
        assertTrue(repository.buscarPorId("no-existe").isEmpty());
    }
 
    @Test
    @DisplayName("actualizarTokenFcm persiste el token en BD y se puede recuperar")
    void actualizarTokenFcmPersiste() {
        String tokenFcm = "ePWiK3M7AbcDef123456";
 
        repository.actualizarTokenFcm("den-test-001", tokenFcm);
 
        Denunciante tras = repository
            .buscarPorId("den-test-001")
            .orElseThrow();
 
        assertEquals(tokenFcm, tras.getTokenFcm());
        assertTrue(tras.tieneTokenFcm());
    }
 
    @Test
    @DisplayName("actualizarTokenFcm sobreescribe un token previo")
    void actualizarSobreescribeToken() {
        repository.actualizarTokenFcm("den-test-001", "token-viejo");
        repository.actualizarTokenFcm("den-test-001", "token-nuevo");
 
        String tokenFinal = repository
            .buscarPorId("den-test-001")
            .orElseThrow()
            .getTokenFcm();
 
        assertEquals("token-nuevo", tokenFinal);
    }

    // ── Épica 8 (hallazgo #6, Parte 2) ──────────────────────────────────

    @Test
    @DisplayName("buscarPorCorreo encuentra al denunciante sembrado por su correo")
    void buscarPorCorreoEncuentra() {
        Optional<Denunciante> resultado =
            repository.buscarPorCorreo("juan@test.com");

        assertTrue(resultado.isPresent());
        assertEquals("den-test-001", resultado.get().getId());
    }

    @Test
    @DisplayName("buscarPorCorreo retorna vacío si ningún denunciante tiene ese correo")
    void buscarPorCorreoInexistente() {
        assertTrue(repository.buscarPorCorreo("no-existe@callsos.test").isEmpty());
    }
}