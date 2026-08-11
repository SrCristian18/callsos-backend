/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteAdministrativo;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.IncidenteRepositoryMySQL;
import com.callsos.backend.infrastructure.adapter.out.persistence.DenunciaRepositoryMySQL;
import com.callsos.backend.infrastructure.adapter.out.persistence.ReporteAdministrativoRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — "Tests de repositorios faltantes: ReporteAdministrativoRepositoryMySQL".
 *
 * NOTA IMPORTANTE — deuda técnica ya documentada en el propio adaptador:
 * buscarPorIncidente() es un stub que SIEMPRE retorna List.of()
 * ("reconstitución pendiente Fase 2"). guardar() sí persiste
 * correctamente. Este test verifica guardar() con una consulta JDBC
 * directa (no vía buscarPorIncidente, que no sirve para eso todavía) y
 * deja constancia explícita del stub para que no se olvide ni se rompa
 * en silencio si alguien lo "arregla" sin darse cuenta de que cambia
 * el contrato.
 */
@JdbcTest
@Import({
    ReporteAdministrativoRepositoryMySQL.class,
    IncidenteRepositoryMySQL.class,
    DenunciaRepositoryMySQL.class
})
@ActiveProfiles("test")
@DisplayName("ReporteAdministrativoRepositoryMySQL — integración H2")
class ReporteAdministrativoRepositoryMySQLTest {

    @Autowired private ReporteAdministrativoRepositoryMySQL repository;
    @Autowired private IncidenteRepositoryMySQL incidenteRepo;
    @Autowired private JdbcTemplate jdbc;

    private final Ubicacion ubicacion = new Ubicacion(10.41, -75.54);
    private final Denunciante denunciante = new Denunciante(
        "den-test-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
    private final UnidadPolicial autoridad = new UnidadPolicial(
        "cai-test-001", "CAI Test Manga", "Calle Test 1", ubicacion, "6010000");

    @Test
    @DisplayName("guardar persiste el reporte administrativo en BD")
    void guardarPersisteEnBD() {
        Incidente incidente = new Incidente(
            "i-repadmin-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        ReporteAdministrativo reporte = new ReporteAdministrativo(
            "ra-001", "Resumen mensual de incidentes en la zona", incidente, autoridad);
        repository.guardar(reporte);

        String resumenPersistido = jdbc.queryForObject(
            "SELECT resumen FROM reportes_administrativos WHERE id = ?",
            String.class, "ra-001");

        assertEquals("Resumen mensual de incidentes en la zona", resumenPersistido);
    }

    @Test
    @DisplayName("guardar sobre un reporte existente actualiza el resumen (ON DUPLICATE KEY)")
    void guardarActualizaResumenExistente() {
        Incidente incidente = new Incidente(
            "i-repadmin-002", TipoIncidente.ATENTADOS, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        repository.guardar(new ReporteAdministrativo("ra-002", "Borrador", incidente, autoridad));
        repository.guardar(new ReporteAdministrativo("ra-002", "Resumen final", incidente, autoridad));

        Integer filas = jdbc.queryForObject(
            "SELECT COUNT(*) FROM reportes_administrativos WHERE id = ?",
            Integer.class, "ra-002");
        String resumen = jdbc.queryForObject(
            "SELECT resumen FROM reportes_administrativos WHERE id = ?",
            String.class, "ra-002");

        assertEquals(1, filas, "No debe duplicar la fila, solo actualizarla");
        assertEquals("Resumen final", resumen);
    }

    @Test
    @DisplayName("DEUDA TÉCNICA DOCUMENTADA: buscarPorIncidente siempre retorna vacío (stub, Fase 2 pendiente)")
    void buscarPorIncidenteEsUnStubQueSiempreRetornaVacio() {
        Incidente incidente = new Incidente(
            "i-repadmin-003", TipoIncidente.RUIDO_EXCESIVO, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        repository.guardar(new ReporteAdministrativo(
            "ra-003", "Este reporte SÍ existe en BD", incidente, autoridad));

        // Documentamos el comportamiento actual a propósito: si este test
        // empieza a fallar porque buscarPorIncidente ya no retorna vacío,
        // significa que alguien implementó la reconstitución pendiente —
        // en ese caso, hay que REEMPLAZAR este test por uno que valide el
        // contenido real (como se hizo con AsignacionRepositoryMySQL en
        // esta misma Épica), no solo borrar el assert.
        List<ReporteAdministrativo> resultado = repository.buscarPorIncidente("i-repadmin-003");
        assertTrue(resultado.isEmpty(),
            "buscarPorIncidente es un stub (Fase 2 pendiente) — si esto falla, " +
            "actualiza este test para reflejar la implementación real.");
    }
}
