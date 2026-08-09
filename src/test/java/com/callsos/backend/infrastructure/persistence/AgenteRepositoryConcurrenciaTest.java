/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

import com.callsos.backend.infrastructure.adapter.out.persistence.AgenteRepositoryMySQL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

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
@DisplayName("AgenteRepositoryMySQL — condición de carrera en intentarReservar (Épica 4)")
class AgenteRepositoryConcurrenciaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Solo UN hilo gana la reserva cuando varios compiten por el mismo agente")
    void soloUnoGanaLaReservaBajoConcurrenciaReal() throws InterruptedException {
        final int hilos = 10;
        final String agenteId = "ag-test-001"; // seed data-test.sql: DISPONIBLE, cai-test-001

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

        boolean primeraReserva = repository.intentarReservar("ag-test-001");
        boolean segundaReserva = repository.intentarReservar("ag-test-001");

        assertTrue(primeraReserva, "La primera reserva sobre un agente DISPONIBLE debe ganar");
        assertTrue(!segundaReserva,
            "Una segunda reserva sobre el mismo agente, ya OCUPADO, debe fallar");
    }
}
