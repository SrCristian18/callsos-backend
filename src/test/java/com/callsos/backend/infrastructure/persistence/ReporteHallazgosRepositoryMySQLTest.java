/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.DenunciaRepositoryMySQL;
import com.callsos.backend.infrastructure.adapter.out.persistence.IncidenteRepositoryMySQL;
import com.callsos.backend.infrastructure.adapter.out.persistence.ReporteHallazgosRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — "Tests de repositorios faltantes: ReporteHallazgosRepositoryMySQL".
 *
 * Requiere que existan reportes_hallazgos en el schema de test — se agregó
 * junto con este test (ver schema-test.sql), ya que faltaba a pesar de
 * existir en producción (database/01_schema.sql).
 */
@JdbcTest
@Import({
    ReporteHallazgosRepositoryMySQL.class,
    IncidenteRepositoryMySQL.class,
    DenunciaRepositoryMySQL.class
})
@ActiveProfiles("test")
@DisplayName("ReporteHallazgosRepositoryMySQL — integración H2")
class ReporteHallazgosRepositoryMySQLTest {

    @Autowired private ReporteHallazgosRepositoryMySQL repository;
    @Autowired private IncidenteRepositoryMySQL incidenteRepo;

    private final Ubicacion ubicacion = new Ubicacion(10.41, -75.54);
    private final Denunciante denunciante = new Denunciante(
        "den-test-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
    private final Agente agente = new Agente(
        "ag-test-001", "Pedro Test", "Av. Test", ubicacion, "3002222222");

    @Test
    @DisplayName("guardar y buscarPorIncidente — ciclo completo")
    void guardarYBuscarPorIncidente() {
        Incidente incidente = new Incidente(
            "i-hallazgo-001", TipoIncidente.INCIDENTE_DE_TRANSITO, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        ReporteHallazgos reporte = new ReporteHallazgos(
            "rh-001", "Vehículo abandonado, sin víctimas", incidente, agente);
        repository.guardar(reporte);

        List<ReporteHallazgos> encontrados = repository.buscarPorIncidente("i-hallazgo-001");

        assertEquals(1, encontrados.size());
        assertEquals("rh-001", encontrados.get(0).getId());
        assertEquals("Vehículo abandonado, sin víctimas", encontrados.get(0).getDescripcion());
        assertEquals("i-hallazgo-001", encontrados.get(0).getIncidente().getId());
        assertEquals("ag-test-001", encontrados.get(0).getAgente().getId());
    }

    @Test
    @DisplayName("buscarPorIncidente retorna vacío si no hay reportes")
    void buscarPorIncidenteSinReportes() {
        List<ReporteHallazgos> encontrados = repository.buscarPorIncidente("no-existe-xyz");
        assertTrue(encontrados.isEmpty());
    }

    @Test
    @DisplayName("buscarPorIncidente ordena los reportes por fecha descendente")
    void ordenPorFechaDescendente() throws InterruptedException {
        Incidente incidente = new Incidente(
            "i-hallazgo-002", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        repository.guardar(new ReporteHallazgos("rh-002-a", "Primer reporte", incidente, agente));
        Thread.sleep(5); // asegura fecha estrictamente posterior
        repository.guardar(new ReporteHallazgos("rh-002-b", "Segundo reporte", incidente, agente));

        List<ReporteHallazgos> encontrados = repository.buscarPorIncidente("i-hallazgo-002");

        assertEquals(2, encontrados.size());
        assertEquals("rh-002-b", encontrados.get(0).getId(), "El más reciente debe ir primero");
        assertEquals("rh-002-a", encontrados.get(1).getId());
    }

    @Test
    @DisplayName("guardar sobre un reporte existente actualiza la descripción (ON DUPLICATE KEY)")
    void guardarActualizaDescripcionExistente() {
        Incidente incidente = new Incidente(
            "i-hallazgo-003", TipoIncidente.RIÑAS_O_PELEAS, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        repository.guardar(new ReporteHallazgos("rh-003", "Borrador inicial", incidente, agente));
        repository.guardar(new ReporteHallazgos("rh-003", "Descripción final revisada", incidente, agente));

        List<ReporteHallazgos> encontrados = repository.buscarPorIncidente("i-hallazgo-003");

        assertEquals(1, encontrados.size(), "No debe duplicar la fila, solo actualizarla");
        assertEquals("Descripción final revisada", encontrados.get(0).getDescripcion());
    }
}
