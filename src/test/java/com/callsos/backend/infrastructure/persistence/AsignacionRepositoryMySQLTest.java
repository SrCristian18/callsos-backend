/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.enums.EstadoAsignacion;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.AsignacionRepositoryMySQL;
import com.callsos.backend.infrastructure.adapter.out.persistence.DenunciaRepositoryMySQL;
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
 * Épica 4 (ruta técnica) — "Tests de repositorios faltantes: AsignacionRepositoryMySQL".
 *
 * Reutiliza los IDs semilla de data-test.sql (den-test-001, ag-test-001)
 * para no necesitar insertar denunciantes/agentes manualmente — solo se
 * inserta lo que el test realmente ejercita (Incidente y Denuncia), igual
 * que hace IncidenteRepositoryMySQLTest.
 */
@JdbcTest
@Import({
    AsignacionRepositoryMySQL.class,
    IncidenteRepositoryMySQL.class,
    DenunciaRepositoryMySQL.class
})
@ActiveProfiles("test")
@DisplayName("AsignacionRepositoryMySQL — integración H2")
class AsignacionRepositoryMySQLTest {

    @Autowired private AsignacionRepositoryMySQL asignacionRepo;
    @Autowired private IncidenteRepositoryMySQL incidenteRepo;
    @Autowired private DenunciaRepositoryMySQL denunciaRepo;

    private final Ubicacion ubicacion = new Ubicacion(10.41, -75.54);
    private final Denunciante denunciante = new Denunciante(
        "den-test-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    private Denuncia crearDenunciaPersistida(String incidenteId, String denunciaId) {
        Incidente incidente = new Incidente(
            incidenteId, TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        Denuncia denuncia = new Denuncia(
            denunciaId, TipoIncidente.ROBOS_O_ASALTOS, "desc denuncia",
            ubicacion, denunciante, incidente);
        denunciaRepo.guardar(denuncia);
        return denuncia;
    }

    @Test
    @DisplayName("guardar y buscarPorIncidente — ciclo completo")
    void guardarYBuscarPorIncidente() {
        Denuncia denuncia = crearDenunciaPersistida("i-asig-001", "den-reg-001");
        Agente agente = new Agente("ag-test-001", "Pedro Test", "Av. Test", ubicacion, "3002222222");

        Asignacion asignacion = new Asignacion("asig-001", agente, denuncia);
        asignacionRepo.guardar(asignacion);

        Optional<Asignacion> encontrada = asignacionRepo.buscarPorIncidente("i-asig-001");

        assertTrue(encontrada.isPresent());
        assertEquals("asig-001", encontrada.get().getId());
        assertEquals(EstadoAsignacion.ACTIVA, encontrada.get().getEstado());
        assertEquals("ag-test-001", encontrada.get().getAgente().getId());
        // Limitación documentada en AsignacionRepositoryMySQL: la reconstitución
        // no arma la Denuncia completa (requeriría JOIN adicional, Fase 2).
        assertNull(encontrada.get().getDenuncia());
    }

    @Test
    @DisplayName("buscarPorIncidente retorna vacío si el incidente no tiene asignación")
    void buscarPorIncidenteSinAsignacion() {
        Optional<Asignacion> resultado = asignacionRepo.buscarPorIncidente("no-existe-xyz");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("buscarPorIncidente solo considera asignaciones ACTIVA")
    void buscarPorIncidenteIgnoraFinalizadas() {
        Denuncia denuncia = crearDenunciaPersistida("i-asig-002", "den-reg-002");
        Agente agente = new Agente("ag-test-001", "Pedro Test", "Av. Test", ubicacion, "3002222222");

        Asignacion asignacion = new Asignacion("asig-002", agente, denuncia);
        asignacion.finalizar(); // ACTIVA -> FINALIZADA
        asignacionRepo.guardar(asignacion);

        Optional<Asignacion> resultado = asignacionRepo.buscarPorIncidente("i-asig-002");
        assertTrue(resultado.isEmpty(),
            "Una asignación FINALIZADA no debe aparecer como asignación activa del incidente");
    }

    @Test
    @DisplayName("guardar sobre una asignación existente actualiza el estado (ON DUPLICATE KEY)")
    void guardarActualizaEstadoExistente() {
        Denuncia denuncia = crearDenunciaPersistida("i-asig-003", "den-reg-003");
        Agente agente = new Agente("ag-test-001", "Pedro Test", "Av. Test", ubicacion, "3002222222");

        Asignacion asignacion = new Asignacion("asig-003", agente, denuncia);
        asignacionRepo.guardar(asignacion); // INSERT inicial, estado ACTIVA

        asignacion.finalizar();
        asignacionRepo.guardar(asignacion); // debe actualizar, no duplicar

        assertTrue(asignacionRepo.buscarPorIncidente("i-asig-003").isEmpty(),
            "Tras el UPDATE la asignación ya no debe figurar como ACTIVA");
    }

    @Test
    @DisplayName("tieneAsignacionActiva refleja el estado real en BD")
    void tieneAsignacionActiva() {
        Denuncia denuncia = crearDenunciaPersistida("i-asig-004", "den-reg-004");
        Agente agente = new Agente("ag-test-001", "Pedro Test", "Av. Test", ubicacion, "3002222222");

        assertFalse(asignacionRepo.tieneAsignacionActiva("i-asig-004"));

        Asignacion asignacion = new Asignacion("asig-004", agente, denuncia);
        asignacionRepo.guardar(asignacion);

        assertTrue(asignacionRepo.tieneAsignacionActiva("i-asig-004"));
    }
}