/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service.support;

import com.callsos.backend.domain.enums.EstadoAgente;
import com.callsos.backend.domain.enums.EstadoAsignacion;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FIX de producción: el agente quedaba OCUPADO en BD para siempre tras
 * cerrar un incidente (finalizar o cancelar) — ver el docstring de
 * {@link AgenteLiberador} para el detalle completo del bug.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgenteLiberador")
class AgenteLiberadorTest {

    @Mock AsignacionRepositoryPort asignacionRepository;
    @Mock AgenteRepositoryPort agenteRepository;

    AgenteLiberador agenteLiberador;

    private final Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

    @BeforeEach
    void setUp() {
        agenteLiberador = new AgenteLiberador(asignacionRepository, agenteRepository);
    }

    private Asignacion asignacionActivaCon(Agente agente) {
        return Asignacion.reconstituir(
            "as-001", LocalDateTime.now(), EstadoAsignacion.ACTIVA, agente, null);
    }

    @Test
    @DisplayName("con asignación ACTIVA: libera al agente (DISPONIBLE) y persiste ambos lados")
    void liberaAgenteConAsignacionActiva() {
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");
        Asignacion asignacion = asignacionActivaCon(agente);
        when(asignacionRepository.buscarPorIncidente("i-001"))
            .thenReturn(Optional.of(asignacion));

        agenteLiberador.liberarSiHayAsignacionActiva("i-001");

        // Efecto en memoria: la Asignacion.finalizar() interna hizo su trabajo.
        assertEquals(EstadoAsignacion.FINALIZADA, asignacion.getEstado());
        assertEquals(EstadoAgente.DISPONIBLE, agente.getEstado());
        assertTrue(agente.estaDisponible());

        // Persistencia: AMBOS lados, no solo uno — es justo el bug que
        // este colaborador corrige (antes ninguno de los dos se
        // persistía; un fix a medias que solo tocara uno de los dos
        // dejaría el otro dato inconsistente).
        ArgumentCaptor<Asignacion> asignacionCaptor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).guardar(asignacionCaptor.capture());
        assertSame(asignacion, asignacionCaptor.getValue());

        ArgumentCaptor<Agente> agenteCaptor = ArgumentCaptor.forClass(Agente.class);
        verify(agenteRepository).actualizarEstado(agenteCaptor.capture());
        assertSame(agente, agenteCaptor.getValue());
    }

    @Test
    @DisplayName("sin asignación activa: no-op silencioso, no lanza excepción ni persiste nada")
    void noOpSinAsignacionActiva() {
        when(asignacionRepository.buscarPorIncidente("i-002")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> agenteLiberador.liberarSiHayAsignacionActiva("i-002"));

        verify(asignacionRepository, never()).guardar(any());
        verifyNoInteractions(agenteRepository);
    }
}
