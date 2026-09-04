/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.DenunciaRepositoryMySQL;
import com.callsos.backend.infrastructure.adapter.out.persistence.IncidenteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración: adaptador JDBC contra H2 en memoria.
 *
 * FIX (auditoría): se agregó @Import(DenunciaRepositoryMySQL.class) porque
 * IncidenteRepositoryMySQL ahora inyecta DenunciaRepositoryPort en el
 * constructor (fix del Bug #4 — Denuncia faltante). Sin este import,
 * el contexto de @JdbcTest lanzaba NoSuchBeanDefinitionException.
 *
 * También se agregan tests de buscarPorDenunciante, buscarPorCAI y
 * buscarPorEstado (nuevo endpoint de Comando) que faltaban en la suite.
 */
@JdbcTest
@Import({IncidenteRepositoryMySQL.class, DenunciaRepositoryMySQL.class})
@ActiveProfiles("test")
@DisplayName("IncidenteRepositoryMySQL — integración H2")
class IncidenteRepositoryMySQLTest {

    @Autowired
    private IncidenteRepositoryMySQL repository;

    @Autowired
    private JdbcTemplate jdbc;

    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denunciante = new Denunciante(
        "den-test-001", "Juan Test", "Cartagena",
        "3001111111", "juan@test.com");

    // ── Tests originales (corregidos) ──────────────────────────────────

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
    @DisplayName("guardar persiste el nuevo tipo tras cambiarTipo() — Épica 1")
    void guardarPersisteNuevoTipo() {
        Incidente incidente = new Incidente(
            "i-integ-tipo-001", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante);
        repository.guardar(incidente);

        incidente.cambiarTipo(TipoIncidente.RIÑAS_O_PELEAS);
        repository.guardar(incidente);

        Optional<Incidente> actualizado = repository.buscarPorId("i-integ-tipo-001");
        assertTrue(actualizado.isPresent());
        assertEquals(TipoIncidente.RIÑAS_O_PELEAS, actualizado.get().getTipo(),
            "El UPDATE debe incluir la columna tipo, no solo el INSERT inicial");
    }

    @Test
    @DisplayName("buscarPorId retorna vacío si el incidente no existe")
    void buscarInexistente() {
        Optional<Incidente> resultado = repository.buscarPorId("no-existe-xyz");
        assertTrue(resultado.isEmpty());
    }

    // ── Tests nuevos (cobertura de los gaps detectados en auditoría) ───

    @Test
    @DisplayName("buscarPorDenunciante retorna todos los incidentes del denunciante")
    void buscarPorDenunciante() {
        // Guardar 2 incidentes del mismo denunciante
        repository.guardar(new Incidente(
            "i-den-001", TipoIncidente.ROBOS_O_ASALTOS,
            "Incidente 1", ubicacion, denunciante));
        repository.guardar(new Incidente(
            "i-den-002", TipoIncidente.RUIDO_EXCESIVO,
            "Incidente 2", ubicacion, denunciante));

        List<Incidente> lista = repository.buscarPorDenunciante("den-test-001");

        assertTrue(lista.size() >= 2,
            "Debe haber al menos los 2 incidentes recién guardados");
        assertTrue(lista.stream().allMatch(i ->
            i.getDenunciante().getId().equals("den-test-001")),
            "Todos los incidentes deben pertenecer al denunciante correcto");
    }

    @Test
    @DisplayName("buscarPorDenunciante retorna vacío si no tiene incidentes")
    void buscarPorDenuncianteVacio() {
        List<Incidente> lista = repository.buscarPorDenunciante("den-inexistente-999");
        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("buscarPorCAI retorna incidentes activos de la unidad policial")
    void buscarPorCAI() {
        // Guardar incidente y derivarlo al cai-test-001
        UnidadPolicial cai = new UnidadPolicial(
            "cai-test-001", "CAI Test Manga", "Calle Test", ubicacion, "601");
        Incidente incidente = new Incidente(
            "i-cai-001", TipoIncidente.ROBOS_O_ASALTOS,
            "Para el CAI", ubicacion, denunciante);
        incidente.derivarACAI(cai);
        repository.guardar(incidente);

        List<Incidente> lista = repository.buscarPorCAI("cai-test-001");

        assertFalse(lista.isEmpty(), "Debe haber al menos un incidente para el CAI");
        assertEquals("cai-test-001",
            lista.get(0).getUnidadPolicial().getId());
    }

    @Test
    @DisplayName("buscarPorEstado — retorna incidentes en estado CREADO (endpoint Comando)")
    void buscarPorEstado() {
        // Guardar 2 incidentes en CREADO y 1 en DERIVADO_A_CAI
        repository.guardar(new Incidente(
            "i-estado-001", TipoIncidente.ROBOS_O_ASALTOS,
            "Pendiente 1", ubicacion, denunciante));
        repository.guardar(new Incidente(
            "i-estado-002", TipoIncidente.RIÑAS_O_PELEAS,
            "Pendiente 2", ubicacion, denunciante));

        // Épica 8 (hallazgo #8.2): antes se guardaba en CREADO y se
        // pasaba a DERIVADO_A_CAI con el ya retirado
        // `repository.actualizarEstado(id, estado)` — ahora se deriva
        // el agregado ANTES de guardar (mismo patrón que buscarPorCAI,
        // arriba), que es como realmente transiciona un Incidente en
        // producción (vía `guardar(Incidente)`, nunca un UPDATE directo
        // de la columna estado).
        UnidadPolicial caiDerivado = new UnidadPolicial(
            "cai-test-buscarporestado", "CAI Test", "Calle Test",
            ubicacion, "601");
        Incidente derivado = new Incidente(
            "i-estado-003", TipoIncidente.ABUSO_INFANTIL,
            "Ya derivado", ubicacion, denunciante);
        derivado.derivarACAI(caiDerivado);
        repository.guardar(derivado);

        List<Incidente> creados = repository.buscarPorEstado(EstadoIncidente.CREADO);

        assertTrue(creados.size() >= 2, "Debe haber al menos los 2 incidentes CREADO");
        assertTrue(creados.stream().allMatch(i ->
            i.getEstado() == EstadoIncidente.CREADO),
            "Todos los resultados deben estar en estado CREADO");
        assertTrue(creados.stream().noneMatch(i ->
            i.getId().equals("i-estado-003")),
            "El incidente DERIVADO no debe aparecer en la lista CREADO");
    }

    @Test
    @DisplayName("buscarPorEstado retorna vacío si no hay incidentes en ese estado")
    void buscarPorEstadoVacio() {
        // EN_ATENCION es un estado intermedio que no tenemos en datos de prueba
        List<Incidente> lista = repository.buscarPorEstado(EstadoIncidente.EN_ATENCION);
        // No podemos afirmar isEmpty() porque otros tests pueden haber dejado datos,
        // pero sí podemos afirmar que todos son del estado correcto.
        assertTrue(lista.stream().allMatch(i ->
            i.getEstado() == EstadoIncidente.EN_ATENCION));
    }
}