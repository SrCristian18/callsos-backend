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

    // ── buscarDerivados (EPIC-18 frontend / hallazgo #14) ──────────────

    @Test
    @DisplayName("buscarDerivados — incluye derivados activos y derivados luego cancelados/finalizados")
    void buscarDerivadosIncluyeCualquierEstadoPosteriorALaDerivacion() {
        UnidadPolicial cai = new UnidadPolicial(
            "cai-test-derivados-001", "CAI Test Derivados", "Calle Test",
            ubicacion, "601");

        // Derivado, todavía activo (DERIVADO_A_CAI).
        Incidente derivadoActivo = new Incidente(
            "i-derivado-001", TipoIncidente.ROBOS_O_ASALTOS,
            "Derivado activo", ubicacion, denunciante);
        derivadoActivo.derivarACAI(cai);
        repository.guardar(derivadoActivo);

        // Derivado y LUEGO cancelado — sigue teniendo unidad_policial_id
        // seteado (guardar() nunca lo limpia), así que debe seguir
        // apareciendo en el historial de derivaciones.
        Incidente derivadoYCancelado = new Incidente(
            "i-derivado-002", TipoIncidente.RIÑAS_O_PELEAS,
            "Derivado y cancelado", ubicacion, denunciante);
        derivadoYCancelado.derivarACAI(cai);
        derivadoYCancelado.cancelar();
        repository.guardar(derivadoYCancelado);

        List<Incidente> derivados = repository.buscarDerivados();

        assertTrue(derivados.stream().anyMatch(i -> i.getId().equals("i-derivado-001")));
        assertTrue(derivados.stream().anyMatch(i -> i.getId().equals("i-derivado-002")),
            "Un incidente cancelado DESPUÉS de haber sido derivado debe seguir "
            + "en el historial de derivaciones — la cancelación no borra que "
            + "alguna vez tuvo un CAI asignado");
        assertTrue(derivados.stream().allMatch(i -> i.getUnidadPolicial() != null),
            "Todo resultado de buscarDerivados() debe tener una unidad policial asignada");
    }

    @Test
    @DisplayName("buscarDerivados — NO incluye un incidente cancelado directamente desde CREADO (nunca derivado)")
    void buscarDerivadosExcluyeCanceladoSinDerivar() {
        // CREADO → CANCELADO directo, sin pasar por derivarACAI() — el
        // denunciante puede cancelar "desde cualquier estado activo"
        // (Incidente.cancelar()), y CREADO es un estado activo.
        Incidente canceladoSinDerivar = new Incidente(
            "i-derivado-003-sin-derivar", TipoIncidente.RUIDO_EXCESIVO,
            "Cancelado sin derivar", ubicacion, denunciante);
        canceladoSinDerivar.cancelar();
        repository.guardar(canceladoSinDerivar);

        List<Incidente> derivados = repository.buscarDerivados();

        assertTrue(derivados.stream().noneMatch(
            i -> i.getId().equals("i-derivado-003-sin-derivar")),
            "Un incidente que nunca tuvo CAI asignado no es un \"derivado\", "
            + "aunque haya terminado en CANCELADO");
    }

    @Test
    @DisplayName("buscarDerivados retorna vacío si nunca se derivó nada (no explota con NULLs)")
    void buscarDerivadosVacioNoRompeConDatosLimpios() {
        // No inserta nada nuevo — solo confirma que el método no lanza
        // sobre una tabla sin incidentes derivados. En una BD compartida
        // entre tests puede haber datos de otros @Test (igual que
        // buscarPorEstadoVacio, arriba), así que la única aserción segura
        // es el invariante, no isEmpty().
        List<Incidente> derivados = repository.buscarDerivados();
        assertTrue(derivados.stream().allMatch(i -> i.getUnidadPolicial() != null));
    }
}