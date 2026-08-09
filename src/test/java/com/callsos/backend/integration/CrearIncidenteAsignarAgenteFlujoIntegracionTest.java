/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.integration;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.AsignarAgentePort;
import com.callsos.backend.domain.port.in.AsignarCAIAIncidentePort;
import com.callsos.backend.domain.port.in.CrearIncidentePort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Épica 4 (ruta técnica) — "Test de integración específico para el flujo
 * completo CrearIncidente -> Denuncia -> AsignarAgente".
 *
 * Por qué este test existe con nombre propio, aparte de los tests
 * unitarios de cada servicio: el propio código documenta (ver comentarios
 * de CrearIncidenteService, AsignarAgenteService, IncidenteRepositoryMySQL)
 * un bug crítico histórico ya corregido — CrearIncidenteService creaba el
 * Incidente pero nunca la Denuncia asociada, lo que bloqueaba
 * silenciosamente TODO incidente real más allá de DERIVADO_A_CAI. Los
 * tests unitarios con mocks no habrían atrapado esto: como controlan
 * directamente lo que devuelve incidenteRepository.buscarPorId(), un mock
 * mal configurado con Denuncia=null no distingue "el mock no tiene
 * denuncia" de "el bug real de persistencia perdió la denuncia".
 *
 * Este test usa el contexto REAL de Spring (@SpringBootTest, no mocks) —
 * la única forma de detectar con certeza que:
 *   1. CrearIncidenteService persiste la Denuncia de verdad.
 *   2. IncidenteRepositoryMySQL la recarga correctamente al reconstruir
 *      el Incidente desde BD (que es exactamente lo que hace
 *      AsignarAgenteService al cargar el incidente por su cuenta).
 *   3. El flujo completo Crear -> Derivar -> Asignar funciona de punta a
 *      punta contra una base de datos real (H2), no contra dobles de test.
 *
 * @Transactional a nivel de clase: cada test corre en su propia
 * transacción, revertida al final — evita que un test deje datos que
 * contaminen a los demás (ej. ag-test-001 quedando OCUPADO).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("Flujo completo: CrearIncidente -> Denuncia -> AsignarAgente (integración real)")
class CrearIncidenteAsignarAgenteFlujoIntegrationTest {

    @Autowired private CrearIncidentePort crearIncidentePort;
    @Autowired private AsignarCAIAIncidentePort asignarCAIAIncidentePort;
    @Autowired private AsignarAgentePort asignarAgentePort;

    @Autowired private IncidenteRepositoryPort incidenteRepository;
    @Autowired private AgenteRepositoryPort agenteRepository;
    @Autowired private AsignacionRepositoryPort asignacionRepository;

    // Coordenadas MUY cercanas a cai-test-001 (10.41, -75.54) — a propósito
    // NO idénticas: con coordenadas exactamente iguales, la fórmula de
    // Haversine en UnidadPolicialRepositoryMySQL puede producir
    // ACOS(ligeramente > 1) por precisión de punto flotante (COS²+SIN²
    // rara vez da exactamente 1.0 en aritmética de doble precisión),
    // lo que algunos motores devuelven como NaN. No hace falta coincidir
    // exacto de todas formas: es el único CAI en los datos semilla, así
    // que cualquier ubicación cercana razonable lo encuentra igual.
    private static final Ubicacion UBICACION_CAI_TEST = new Ubicacion(10.4102, -75.5398);

    @Test
    @DisplayName("Flujo feliz completo: crear -> derivar -> asignar, estado final correcto")
    void flujoCompletoDePuntaAPunta() {

        // 1. CREAR — usa den-test-001 (seed data-test.sql)
        Incidente creado = crearIncidentePort.ejecutar(
            "den-test-001",
            TipoIncidente.ROBOS_O_ASALTOS,
            "Robo reportado en integración",
            UBICACION_CAI_TEST
        );

        // Regresión del bug histórico: el Incidente devuelto DEBE tener
        // su Denuncia ya vinculada en memoria, inmediatamente tras crear.
        assertNotNull(creado.getDenuncia(),
            "REGRESIÓN DEL BUG HISTÓRICO: CrearIncidenteService debe crear " +
            "y vincular la Denuncia — sin esto, AsignarAgenteService falla " +
            "para CUALQUIER incidente real.");
        assertEquals(EstadoIncidente.CREADO, creado.getEstado());

        // Verificación más fuerte todavía: recargar desde BD (no confiar en
        // el objeto en memoria) — esto es exactamente lo que hace
        // AsignarAgenteService internamente al cargar el incidente por su
        // propia cuenta, en un caso de uso completamente separado.
        Incidente recargado = incidenteRepository.buscarPorId(creado.getId())
            .orElseThrow(() -> new AssertionError("El incidente no se persistió"));
        assertNotNull(recargado.getDenuncia(),
            "REGRESIÓN DEL BUG HISTÓRICO: la Denuncia debe sobrevivir un " +
            "ciclo completo de guardar/recargar desde BD, no solo existir " +
            "en el objeto en memoria recién creado.");

        // 2. DERIVAR — Comando deriva el incidente al CAI más cercano
        asignarCAIAIncidentePort.ejecutar(creado.getId());

        Incidente derivado = incidenteRepository.buscarPorId(creado.getId()).orElseThrow();
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, derivado.getEstado());
        assertNotNull(derivado.getUnidadPolicial());
        assertEquals("cai-test-001", derivado.getUnidadPolicial().getId());

        // 3. ASIGNAR — el paso que el bug histórico bloqueaba por completo
        asignarAgentePort.ejecutar(creado.getId());

        Incidente asignado = incidenteRepository.buscarPorId(creado.getId()).orElseThrow();
        assertEquals(EstadoIncidente.AGENTE_ASIGNADO, asignado.getEstado());

        // El agente semilla (ag-test-001, único DISPONIBLE en cai-test-001)
        // debe haber quedado OCUPADO.
        List<Agente> disponiblesTrasAsignar = agenteRepository
            .obtenerDisponiblesPorUnidad("cai-test-001");
        assertTrue(disponiblesTrasAsignar.isEmpty(),
            "ag-test-001 era el único agente disponible del CAI — tras " +
            "asignar, la lista de disponibles debe quedar vacía.");

        // La Asignacion debe haber quedado persistida de verdad (no solo
        // en memoria) — este es el FIX 2 documentado en AsignarAgenteService.
        Optional<com.callsos.backend.domain.model.Asignacion> asignacionPersistida =
            asignacionRepository.buscarPorIncidente(creado.getId());
        assertTrue(asignacionPersistida.isPresent(),
            "La Asignacion debe estar persistida en BD, no solo existir " +
            "como objeto en memoria dentro de AsignarAgenteService.");
        assertEquals("ag-test-001", asignacionPersistida.get().getAgente().getId());
    }

    @Test
    @DisplayName("Asignar sin haber derivado primero falla con mensaje claro (orden del flujo)")
    void asignarSinDerivarFalla() {
        Incidente creado = crearIncidentePort.ejecutar(
            "den-test-001", TipoIncidente.RUIDO_EXCESIVO,
            "Ruido reportado", UBICACION_CAI_TEST);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> asignarAgentePort.ejecutar(creado.getId()));

        assertTrue(ex.getMessage().contains("CAI"),
            "El mensaje debe orientar al operador a derivar primero");
    }

    @Test
    @DisplayName("Asignar cuando no hay agentes disponibles falla sin dejar estado inconsistente")
    void asignarSinAgentesDisponiblesFalla() {
        // Reservamos manualmente al único agente semilla ANTES del flujo,
        // simulando que ya está ocupado por otra asignación previa.
        assertTrue(agenteRepository.intentarReservar("ag-test-001"));

        Incidente creado = crearIncidentePort.ejecutar(
            "den-test-001", TipoIncidente.ATENTADOS,
            "Sin agentes disponibles", UBICACION_CAI_TEST);
        asignarCAIAIncidentePort.ejecutar(creado.getId());

        assertThrows(IllegalStateException.class,
            () -> asignarAgentePort.ejecutar(creado.getId()));

        // El incidente debe seguir en DERIVADO_A_CAI, no haber avanzado a
        // un estado a medias.
        Incidente sinCambios = incidenteRepository.buscarPorId(creado.getId()).orElseThrow();
        assertEquals(EstadoIncidente.DERIVADO_A_CAI, sinCambios.getEstado());
    }
}
