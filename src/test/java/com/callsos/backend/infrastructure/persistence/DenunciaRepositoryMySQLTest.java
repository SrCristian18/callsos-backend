/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
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
 * Épica 4 (ruta técnica) — "Tests de repositorios faltantes: DenunciaRepositoryMySQL".
 *
 * Orden de persistencia obligatorio (documentado en el propio adaptador):
 *   1. incidenteRepository.guardar(incidente)
 *   2. denunciaRepository.guardar(denuncia)
 */
@JdbcTest
@Import({DenunciaRepositoryMySQL.class, IncidenteRepositoryMySQL.class})
@ActiveProfiles("test")
@DisplayName("DenunciaRepositoryMySQL — integración H2")
class DenunciaRepositoryMySQLTest {

    @Autowired private DenunciaRepositoryMySQL denunciaRepo;
    @Autowired private IncidenteRepositoryMySQL incidenteRepo;

    private final Ubicacion ubicacion = new Ubicacion(10.41, -75.54);
    private final Denunciante denunciante = new Denunciante(
        "den-test-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @Test
    @DisplayName("guardar y buscarPorIncidente — ciclo completo")
    void guardarYBuscarPorIncidente() {
        Incidente incidente = new Incidente(
            "i-denuncia-001", TipoIncidente.VIOLENCIA_DOMESTICA, "desc incidente",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        Denuncia denuncia = new Denuncia(
            "den-reg-001", TipoIncidente.VIOLENCIA_DOMESTICA, "desc denuncia",
            ubicacion, denunciante, incidente);
        denunciaRepo.guardar(denuncia);

        Optional<Denuncia> encontrada = denunciaRepo.buscarPorIncidente("i-denuncia-001", incidente);

        assertTrue(encontrada.isPresent());
        assertEquals("den-reg-001", encontrada.get().getId());
        assertEquals(TipoIncidente.VIOLENCIA_DOMESTICA, encontrada.get().getTipo());
        assertEquals("desc denuncia", encontrada.get().getDescripcion());
        assertEquals("den-test-001", encontrada.get().getDenunciante().getId());
        assertNotNull(encontrada.get().getUbicacion());
        assertEquals(10.41, encontrada.get().getUbicacion().getLatitud(), 0.0001);
    }

    @Test
    @DisplayName("buscarPorIncidente retorna vacío si el incidente no tiene denuncia registrada")
    void buscarPorIncidenteSinDenuncia() {
        Incidente incidente = new Incidente(
            "i-denuncia-002", TipoIncidente.RUIDO_EXCESIVO, "sin denuncia",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        Optional<Denuncia> resultado = denunciaRepo.buscarPorIncidente("i-denuncia-002", incidente);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("guardar sobre una denuncia existente actualiza la descripción (ON DUPLICATE KEY)")
    void guardarActualizaDescripcionExistente() {
        Incidente incidente = new Incidente(
            "i-denuncia-003", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        Denuncia original = new Denuncia(
            "den-reg-003", TipoIncidente.ROBOS_O_ASALTOS, "descripción original",
            ubicacion, denunciante, incidente);
        denunciaRepo.guardar(original);

        Denuncia actualizada = new Denuncia(
            "den-reg-003", TipoIncidente.ROBOS_O_ASALTOS, "descripción corregida",
            ubicacion, denunciante, incidente);
        denunciaRepo.guardar(actualizada);

        Optional<Denuncia> encontrada = denunciaRepo.buscarPorIncidente("i-denuncia-003", incidente);
        assertTrue(encontrada.isPresent());
        assertEquals("descripción corregida", encontrada.get().getDescripcion());
    }

    @Test
    @DisplayName("buscarPorIncidente reconstituye correctamente el Denunciante asociado")
    void buscarPorIncidenteReconstituyeDenunciante() {
        Incidente incidente = new Incidente(
            "i-denuncia-004", TipoIncidente.ATENTADOS, "desc",
            ubicacion, denunciante);
        incidenteRepo.guardar(incidente);

        Denuncia denuncia = new Denuncia(
            "den-reg-004", TipoIncidente.ATENTADOS, "desc",
            ubicacion, denunciante, incidente);
        denunciaRepo.guardar(denuncia);

        Optional<Denuncia> encontrada = denunciaRepo.buscarPorIncidente("i-denuncia-004", incidente);

        assertTrue(encontrada.isPresent());
        assertEquals("Juan Test", encontrada.get().getDenunciante().getNombre());
        assertEquals("juan@test.com", encontrada.get().getDenunciante().getCorreo());
    }
}
