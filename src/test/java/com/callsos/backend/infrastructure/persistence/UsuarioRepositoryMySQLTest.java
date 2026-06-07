/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.out.UsuarioRepositoryPort.UsuarioCredencial;
import com.callsos.backend.infrastructure.adapter.out.persistence.UsuarioRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Test de integración: UsuarioRepositoryMySQL contra H2 en memoria.
 * Verifica el comportamiento real del SQL incluyendo el filtro activo=TRUE.
 */
@JdbcTest
@Import(UsuarioRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("UsuarioRepositoryMySQL — integración H2")
public class UsuarioRepositoryMySQLTest {
    
    @Autowired
    private UsuarioRepositoryMySQL repository;
 
    @Test
    @DisplayName("Encuentra usuario activo por username")
    void encuentraUsuarioActivo() {
        Optional<UsuarioCredencial> resultado =
            repository.buscarPorUsername("juan.test");
 
        assertTrue(resultado.isPresent());
        UsuarioCredencial cred = resultado.get();
        assertEquals("juan.test",    cred.username());
        assertEquals("DENUNCIANTE",  cred.rol());
        assertEquals("den-test-001", cred.actorId());
        // Verifica que el password es un hash BCrypt (empieza con $2a$)
        assertTrue(cred.password().startsWith("$2a$"));
    }
 
    @Test
    @DisplayName("No encuentra usuario inexistente")
    void noEncuentraInexistente() {
        Optional<UsuarioCredencial> resultado =
            repository.buscarPorUsername("no-existe");
        assertTrue(resultado.isEmpty());
    }
 
    @Test
    @DisplayName("No encuentra usuario con activo=FALSE")
    void noEncuentraUsuarioInactivo() {
        // inactivo.test está en data-test.sql con activo=FALSE
        Optional<UsuarioCredencial> resultado =
            repository.buscarPorUsername("inactivo.test");
        assertTrue(resultado.isEmpty());
    }
 
    @Test
    @DisplayName("actorId es el ID del modelo de negocio, no el ID interno")
    void actorIdEsElModeloDeNegocio() {
        UsuarioCredencial cred = repository
            .buscarPorUsername("juan.test")
            .orElseThrow();
 
        // actorId apunta al denunciante, no al registro de usuario
        assertEquals("den-test-001", cred.actorId());
        assertNotEquals(cred.id(), cred.actorId());
    }
}
