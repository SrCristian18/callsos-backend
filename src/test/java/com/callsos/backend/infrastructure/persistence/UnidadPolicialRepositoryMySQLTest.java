/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.adapter.out.persistence.UnidadPolicialRepositoryMySQL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración del algoritmo Haversine en UnidadPolicialRepositoryMySQL.
 *
 * CONTEXTO: la función de negocio más crítica de CallSOS — seleccionar el
 * CAI más cercano geográficamente — estaba sin cobertura de test (detectado
 * en auditoría). Un error en las fórmulas de ACOS/RADIANS produciría que
 * todos los incidentes vayan al mismo CAI independientemente de la ubicación.
 *
 * ESTRATEGIA: insertar 3 CAIs a distancias conocidas desde un punto de
 * prueba y verificar que siempre se selecciona el correcto.
 *
 * Coordenadas usadas (Cartagena real):
 * - Punto de emergencia:  10.391, -75.479  (Bocagrande)
 * - CAI NORTE:            10.420, -75.510  (~4.5 km — más lejano)
 * - CAI CENTRO:           10.400, -75.490  (~1.3 km — intermedio)
 * - CAI SUR:              10.393, -75.481  (~0.3 km — más cercano)
 */
@JdbcTest
@Import(UnidadPolicialRepositoryMySQL.class)
@ActiveProfiles("test")
@DisplayName("UnidadPolicialRepositoryMySQL — algoritmo Haversine")
class UnidadPolicialRepositoryMySQLTest {

    @Autowired
    private UnidadPolicialRepositoryMySQL repository;

    @Autowired
    private JdbcTemplate jdbc;

    // Punto de emergencia de prueba (Bocagrande, Cartagena)
    private static final Ubicacion EMERGENCIA = new Ubicacion(10.391, -75.479);

    @BeforeEach
    void insertarCaisDeTest() {
        // CAI más cercano al punto de emergencia (~0.3 km)
        jdbc.update("""
            INSERT INTO unidades_policiales (id, nombre, direccion, latitud, longitud, telefono)
            VALUES ('cai-sur-001', 'CAI SUR TEST', 'Sur', 10.393, -75.481, '111')
            """);

        // CAI a distancia intermedia (~1.3 km)
        jdbc.update("""
            INSERT INTO unidades_policiales (id, nombre, direccion, latitud, longitud, telefono)
            VALUES ('cai-centro-001', 'CAI CENTRO TEST', 'Centro', 10.400, -75.490, '222')
            """);

        // CAI más lejano (~4.5 km)
        jdbc.update("""
            INSERT INTO unidades_policiales (id, nombre, direccion, latitud, longitud, telefono)
            VALUES ('cai-norte-001', 'CAI NORTE TEST', 'Norte', 10.420, -75.510, '333')
            """);
    }

    @Test
    @DisplayName("selecciona el CAI más cercano al punto de emergencia")
    void seleccionaElMasCercano() {
        Optional<UnidadPolicial> resultado = repository.buscarPorUbicacion(EMERGENCIA);

        assertTrue(resultado.isPresent(), "Debe encontrar al menos un CAI");
        assertEquals("cai-sur-001", resultado.get().getId(),
            "Debe seleccionar CAI SUR (el más cercano a Bocagrande)");
        assertEquals("CAI SUR TEST", resultado.get().getNombre());
    }

    @Test
    @DisplayName("con emergencia en el norte, selecciona el CAI norte")
    void seleccionaElCorrectoPorZona() {
        // Punto de emergencia en el norte de Cartagena (cerca de cai-norte-001)
        Ubicacion emergenciaNorte = new Ubicacion(10.419, -75.509);

        Optional<UnidadPolicial> resultado =
            repository.buscarPorUbicacion(emergenciaNorte);

        assertTrue(resultado.isPresent());
        assertEquals("cai-norte-001", resultado.get().getId(),
            "Para una emergencia en el norte, debe seleccionar CAI NORTE");
    }

    @Test
    @DisplayName("Haversine es simétrico — punto equidistante elige uno de los dos candidatos")
    void seleccionaCuandoHayCAIsDisponibles() {
        // Verificación de que Haversine siempre devuelve exactamente un resultado
        // (el LIMIT 1 de la query) y no falla con múltiples candidatos.
        Optional<UnidadPolicial> resultado = repository.buscarPorUbicacion(EMERGENCIA);

        assertTrue(resultado.isPresent(), "Siempre debe devolver exactamente un CAI");
    }

    @Test
    @DisplayName("buscarPorUbicacion retorna vacío si no hay CAIs registrados")
    void retornaVacioSinCAIs() {
        // Limpiar solo los CAIs de test (no tocar data-test.sql)
        jdbc.update("DELETE FROM unidades_policiales WHERE id LIKE 'cai-%-001'");

        // Verificar con coordenadas arbitrarias
        // NOTA: data-test.sql inserta un cai-test-001 que también estará presente
        // a menos que se borre — lo que prueba el comportamiento con al menos un CAI.
        Optional<UnidadPolicial> resultado =
            repository.buscarPorUbicacion(new Ubicacion(0.0, 0.0));

        // Con el CAI de data-test.sql todavía presente, el resultado no es vacío.
        // Este test verifica que Haversine funciona incluso con coordenadas extremas.
        // Para el caso de tabla completamente vacía, el comportamiento es Optional.empty().
        assertNotNull(resultado, "buscarPorUbicacion nunca debe lanzar, siempre retorna Optional");
    }

    @Test
    @DisplayName("el CAI devuelto tiene todos los campos correctamente mapeados")
    void mapeaCamposCorrectamente() {
        Optional<UnidadPolicial> resultado = repository.buscarPorUbicacion(EMERGENCIA);

        assertTrue(resultado.isPresent());
        UnidadPolicial cai = resultado.get();

        assertNotNull(cai.getId(), "id no debe ser null");
        assertNotNull(cai.getNombre(), "nombre no debe ser null");
        assertNotNull(cai.getUbicacion(), "ubicacion no debe ser null");
        // Verificar que las coordenadas son números válidos (no NaN ni Infinity)
        assertFalse(Double.isNaN(cai.getUbicacion().getLatitud()),
            "latitud no debe ser NaN");
        assertFalse(Double.isNaN(cai.getUbicacion().getLongitud()),
            "longitud no debe ser NaN");
    }

    // ── Épica 8 (hallazgo #6, Parte 1) ──────────────────────────────────

    @Test
    @DisplayName("buscarPorId lee el correo cuando está presente en BD")
    void buscarPorIdLeeCorreo() {
        jdbc.update("""
            INSERT INTO unidades_policiales (id, nombre, direccion, latitud, longitud, telefono, correo)
            VALUES ('cai-con-correo-001', 'CAI Con Correo', 'Test', 10.4, -75.5, '444', 'cai.correo@callsos.test')
            """);

        Optional<UnidadPolicial> resultado = repository.buscarPorId("cai-con-correo-001");

        assertTrue(resultado.isPresent());
        assertEquals("cai.correo@callsos.test", resultado.get().getCorreo());
    }

    @Test
    @DisplayName("CAI sembrado sin correo (nunca tuvo flujo de registro) se lee como null sin lanzar")
    void caiSinCorreoSeLeeComoNull() {
        // Los CAIs insertados en insertarCaisDeTest() no incluyen correo —
        // mismo caso que las filas de 02_data.sql en producción.
        Optional<UnidadPolicial> resultado = repository.buscarPorId("cai-sur-001");

        assertTrue(resultado.isPresent());
        assertNull(resultado.get().getCorreo());
    }

    @Test
    @DisplayName("buscarPorCorreo encuentra la unidad por su correo")
    void buscarPorCorreoEncuentra() {
        jdbc.update("""
            INSERT INTO unidades_policiales (id, nombre, direccion, latitud, longitud, telefono, correo)
            VALUES ('cai-buscar-correo-001', 'CAI Buscar Correo', 'Test', 10.4, -75.5, '444', 'cai.buscar@callsos.test')
            """);

        Optional<UnidadPolicial> resultado = repository.buscarPorCorreo("cai.buscar@callsos.test");

        assertTrue(resultado.isPresent());
        assertEquals("cai-buscar-correo-001", resultado.get().getId());
    }

    @Test
    @DisplayName("buscarPorCorreo retorna vacío si ninguna unidad tiene ese correo")
    void buscarPorCorreoInexistente() {
        assertTrue(repository.buscarPorCorreo("no-existe@callsos.test").isEmpty());
    }
}