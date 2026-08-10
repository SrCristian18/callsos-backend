/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.infrastructure.adapter.out.persistence.AgenteRepositoryMySQL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Épica 4 (ruta técnica) — "Test/verificación de condición de carrera en
 * AsignarAgenteService".
 *
 * Antes del fix, el flujo era: SELECT (obtenerDisponiblesPorUnidad) seguido
 * de un UPDATE ciego (actualizarEstado) sin ninguna condición ni lock entre
 * medio — dos operadores asignando al mismo tiempo podían leer el mismo
 * agente "disponible" antes de que cualquiera de los dos UPDATE aplicara.
 *
 * Este test dispara concurrencia REAL (hilos + conexiones JDBC separadas,
 * no mocks) contra intentarReservar() — el UPDATE condicional que
 * reemplaza al UPDATE ciego — y verifica que, sin importar cuántos hilos
 * compitan por el mismo agente, únicamente UNO puede ganar la reserva.
 */
@JdbcTest
@Import(AgenteRepositoryMySQL.class)
@ActiveProfiles("test")
// @JdbcTest envuelve cada test en una transacción con rollback automático
// por defecto. La desactivamos aquí a propósito: este test usa hilos con
// conexiones JDBC independientes que necesitan VER commits reales entre sí
// (y el @BeforeEach necesita comitear el agente dedicado antes de que los
// hilos lo lean). Con la transacción de Spring activa, el INSERT del
// @BeforeEach quedaría sin comitear y sería invisible para esas conexiones.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("AgenteRepositoryMySQL — condición de carrera en intentarReservar (Épica 4)")
class AgenteRepositoryConcurrenciaTest {

    @Autowired
    private DataSource dataSource;

    // IDs propios de esta clase — NUNCA "ag-test-001". Ese es el agente
    // semilla compartido en data-test.sql del que dependen otras clases de
    // test (p. ej. AgenteRepositoryMySQLTest). Este test hace commits reales
    // fuera de la transacción de Spring (necesario para simular concurrencia
    // real con conexiones separadas), así que si tocara el agente
    // compartido lo dejaría permanentemente OCUPADO y rompería a las demás
    // clases que corren en el mismo ciclo de `mvn test`.
    private static final String AGENTE_CONCURRENCIA_ID = "ag-concurrencia-001";
    private static final String UNIDAD_TEST_ID = "cai-test-001"; // ya existe en data-test.sql

    @BeforeEach
    void crearAgenteDedicado() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
            """
            INSERT INTO agentes
                (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
            VALUES (?, 'Agente Concurrencia Test', 'N/A', 10.41, -75.54, '3000000000', 'DISPONIBLE', ?)
            """,
            AGENTE_CONCURRENCIA_ID, UNIDAD_TEST_ID
        );
    }

    @AfterEach
    void limpiarAgenteDedicado() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM agentes WHERE id = ?", AGENTE_CONCURRENCIA_ID);
    }

    @Test
    @DisplayName("Solo UN hilo gana la reserva cuando varios compiten por el mismo agente")
    void soloUnoGanaLaReservaBajoConcurrenciaReal() throws InterruptedException {
        final int hilos = 10;
        final String agenteId = AGENTE_CONCURRENCIA_ID;

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger ganadores = new AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                try {
                    // Cada hilo usa su PROPIA instancia de repositorio sobre
                    // el mismo DataSource -> conexión JDBC independiente,
                    // simulando "dos operadores distintos" reales, no dos
                    // llamadas secuenciales sobre la misma conexión.
                    AgenteRepositoryMySQL repoDelHilo = new AgenteRepositoryMySQL(dataSource);

                    listos.countDown();
                    salida.await(5, TimeUnit.SECONDS); // arrancar lo más simultáneo posible

                    if (repoDelHilo.intentarReservar(agenteId)) {
                        ganadores.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(listos.await(5, TimeUnit.SECONDS), "Los hilos no llegaron a tiempo a la línea de salida");
        salida.countDown(); // dispara los N hilos casi al mismo tiempo

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
            "Los hilos no terminaron a tiempo");

        assertEquals(1, ganadores.get(),
            "Exactamente UN hilo debe ganar la reserva. Si este valor es > 1, " +
            "el UPDATE condicional no está protegiendo contra la condición de " +
            "carrera (regresión del fix de la Épica 4). Si es 0, algo más " +
            "impidió que cualquiera reservara.");

        // Verificación adicional: el agente terminó OCUPADO en BD de forma
        // consistente — no quedó en un estado intermedio raro por escrituras
        // concurrentes.
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String estadoFinal = jdbc.queryForObject(
            "SELECT estado FROM agentes WHERE id = ?", String.class, agenteId);
        assertEquals("OCUPADO", estadoFinal);
    }

    @Test
    @DisplayName("Reservar un agente que ya está OCUPADO siempre devuelve false (idempotencia)")
    void reservarAgenteYaOcupadoDevuelveFalse() {
        AgenteRepositoryMySQL repository = new AgenteRepositoryMySQL(dataSource);

        boolean primeraReserva = repository.intentarReservar(AGENTE_CONCURRENCIA_ID);
        boolean segundaReserva = repository.intentarReservar(AGENTE_CONCURRENCIA_ID);

        assertTrue(primeraReserva, "La primera reserva sobre un agente DISPONIBLE debe ganar");
        assertTrue(!segundaReserva,
            "Una segunda reserva sobre el mismo agente, ya OCUPADO, debe fallar");
    }
}