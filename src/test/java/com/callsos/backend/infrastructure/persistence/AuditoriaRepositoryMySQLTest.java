/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.infrastructure.adapter.out.persistence.AuditoriaRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — "Tests de repositorios faltantes: AuditoriaRepositoryMySQL".
 *
 * auditoria_incidente no tiene FK hacia incidentes en el schema de test
 * (es un log de trazabilidad append-only), por lo que este test no
 * necesita depender de IncidenteRepositoryMySQL.
 */
@JdbcTest
@Import(AuditoriaRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("AuditoriaRepositoryMySQL — integración H2")
class AuditoriaRepositoryMySQLTest {

    @Autowired
    private AuditoriaRepositoryMySQL repository;

    @Test
    @DisplayName("registrar y buscarPorIncidente — ciclo completo")
    void registrarYBuscar() {
        AuditoriaIncidente evento = new AuditoriaIncidente(
            "i-audit-001",
            EstadoIncidente.CREADO,
            EstadoIncidente.DERIVADO_A_CAI,
            "usr-comando-001",
            "COMANDO",
            "Derivado al CAI más cercano"
        );

        repository.registrar(evento);

        List<AuditoriaIncidente> historial = repository.buscarPorIncidente("i-audit-001");

        assertEquals(1, historial.size());
        assertEquals(EstadoIncidente.CREADO, historial.get(0).getEstadoAnterior());
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, historial.get(0).getEstadoNuevo());
        assertEquals("usr-comando-001", historial.get(0).getActorId());
        assertEquals("COMANDO", historial.get(0).getActorRol());
        assertEquals("Derivado al CAI más cercano", historial.get(0).getDetalle());
    }

    @Test
    @DisplayName("estadoAnterior null se persiste y reconstituye correctamente (creación inicial)")
    void estadoAnteriorNuloEnCreacion() {
        AuditoriaIncidente creacion = new AuditoriaIncidente(
            "i-audit-002", null, EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE", "Incidente creado");

        repository.registrar(creacion);

        List<AuditoriaIncidente> historial = repository.buscarPorIncidente("i-audit-002");

        assertEquals(1, historial.size());
        assertNull(historial.get(0).getEstadoAnterior());
        assertEquals(EstadoIncidente.CREADO, historial.get(0).getEstadoNuevo());
    }

    @Test
    @DisplayName("buscarPorIncidente retorna el historial ordenado cronológicamente (ASC)")
    void ordenCronologico() {
        repository.registrar(new AuditoriaIncidente(
            "i-audit-003", null, EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE", "Paso 1: creado"));
        repository.registrar(new AuditoriaIncidente(
            "i-audit-003", EstadoIncidente.CREADO, EstadoIncidente.DERIVADO_A_CAI,
            "usr-comando-001", "COMANDO", "Paso 2: derivado"));
        repository.registrar(new AuditoriaIncidente(
            "i-audit-003", EstadoIncidente.DERIVADO_A_CAI, EstadoIncidente.AGENTE_ASIGNADO,
            "usr-cai-001", "CAI", "Paso 3: agente asignado"));

        List<AuditoriaIncidente> historial = repository.buscarPorIncidente("i-audit-003");

        assertEquals(3, historial.size());
        assertEquals("Paso 1: creado", historial.get(0).getDetalle());
        assertEquals("Paso 2: derivado", historial.get(1).getDetalle());
        assertEquals("Paso 3: agente asignado", historial.get(2).getDetalle());
    }

    @Test
    @DisplayName("buscarPorIncidente retorna vacío si no hay auditoría registrada")
    void buscarPorIncidenteSinEventos() {
        List<AuditoriaIncidente> historial = repository.buscarPorIncidente("no-existe-xyz");
        assertTrue(historial.isEmpty());
    }

    @Test
    @DisplayName("buscarPorIncidente no mezcla eventos de otros incidentes")
    void noMezclaIncidentes() {
        repository.registrar(new AuditoriaIncidente(
            "i-audit-004", null, EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE", "Evento incidente 004"));
        repository.registrar(new AuditoriaIncidente(
            "i-audit-005", null, EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE", "Evento incidente 005"));

        List<AuditoriaIncidente> historial004 = repository.buscarPorIncidente("i-audit-004");

        assertEquals(1, historial004.size());
        assertEquals("Evento incidente 004", historial004.get(0).getDetalle());
    }

    // ── Épica 2: cambio de campo genérico (columnas nuevas) ─────────────────

    @Test
    @DisplayName("un cambio de campo genérico (tipo) se persiste y reconstituye con campo/valor_*")
    void registraYReconstituyeCambioGenerico() {
        AuditoriaIncidente cambioTipo = AuditoriaIncidente.deCambioGenerico(
            "i-audit-006", EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE",
            "Tipo actualizado: ROBOS_O_ASALTOS → RIÑAS_O_PELEAS",
            "tipo", "ROBOS_O_ASALTOS", "RIÑAS_O_PELEAS");

        repository.registrar(cambioTipo);

        List<AuditoriaIncidente> historial = repository.buscarPorIncidente("i-audit-006");

        assertEquals(1, historial.size());
        AuditoriaIncidente persistido = historial.get(0);
        assertNull(persistido.getEstadoAnterior(),
            "Un cambio de campo genérico no representa una transición de estado");
        assertEquals(EstadoIncidente.CREADO, persistido.getEstadoNuevo());
        assertEquals("tipo", persistido.getCampo());
        assertEquals("ROBOS_O_ASALTOS", persistido.getValorAnteriorGenerico());
        assertEquals("RIÑAS_O_PELEAS", persistido.getValorNuevoGenerico());
        assertTrue(persistido.esCambioGenerico());
    }

    @Test
    @DisplayName("un cambio de estado normal deja campo/valor_*_generico en NULL")
    void cambioDeEstadoDejaColumnasGenericasNulas() {
        repository.registrar(new AuditoriaIncidente(
            "i-audit-007", EstadoIncidente.CREADO, EstadoIncidente.DERIVADO_A_CAI,
            "usr-comando-001", "COMANDO", "Derivado"));

        AuditoriaIncidente persistido = repository.buscarPorIncidente("i-audit-007").get(0);

        assertNull(persistido.getCampo());
        assertNull(persistido.getValorAnteriorGenerico());
        assertNull(persistido.getValorNuevoGenerico());
        assertFalse(persistido.esCambioGenerico());
    }

    @Test
    @DisplayName("el historial de un incidente mezcla cronológicamente transiciones de estado y cambios de tipo")
    void mezclaTransicionesYCambiosDeTipoEnOrden() {
        repository.registrar(new AuditoriaIncidente(
            "i-audit-008", null, EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE", "Paso 1: creado"));
        repository.registrar(AuditoriaIncidente.deCambioGenerico(
            "i-audit-008", EstadoIncidente.CREADO,
            "den-test-001", "DENUNCIANTE", "Paso 2: tipo actualizado",
            "tipo", "ROBOS_O_ASALTOS", "RIÑAS_O_PELEAS"));
        repository.registrar(new AuditoriaIncidente(
            "i-audit-008", EstadoIncidente.CREADO, EstadoIncidente.DERIVADO_A_CAI,
            "usr-comando-001", "COMANDO", "Paso 3: derivado"));

        List<AuditoriaIncidente> historial = repository.buscarPorIncidente("i-audit-008");

        assertEquals(3, historial.size());
        assertEquals("Paso 1: creado", historial.get(0).getDetalle());
        assertFalse(historial.get(0).esCambioGenerico());
        assertEquals("Paso 2: tipo actualizado", historial.get(1).getDetalle());
        assertTrue(historial.get(1).esCambioGenerico());
        assertEquals("Paso 3: derivado", historial.get(2).getDetalle());
        assertFalse(historial.get(2).esCambioGenerico());
    }
}
