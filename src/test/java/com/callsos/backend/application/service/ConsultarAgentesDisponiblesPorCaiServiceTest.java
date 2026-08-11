/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarAgentesDisponiblesPorCaiService")
class ConsultarAgentesDisponiblesPorCaiServiceTest {

    @Mock AgenteRepositoryPort agenteRepository;

    ConsultarAgentesDisponiblesPorCaiService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarAgentesDisponiblesPorCaiService(agenteRepository);
    }

    @Test
    @DisplayName("delega en agenteRepository.obtenerDisponiblesPorUnidad y retorna el resultado")
    void delegaCorrectamente() {
        Agente agente = new Agente(
            "ag-001", "Pedro", "Av. Test", new Ubicacion(10.4, -75.5), "300");
        when(agenteRepository.obtenerDisponiblesPorUnidad("cai-001"))
            .thenReturn(List.of(agente));

        List<Agente> resultado = service.ejecutar("cai-001");

        assertEquals(1, resultado.size());
        assertEquals("ag-001", resultado.get(0).getId());
        verify(agenteRepository).obtenerDisponiblesPorUnidad("cai-001");
    }

    @Test
    @DisplayName("retorna lista vacía si el CAI no tiene agentes disponibles")
    void listaVacia() {
        when(agenteRepository.obtenerDisponiblesPorUnidad("cai-002"))
            .thenReturn(List.of());

        List<Agente> resultado = service.ejecutar("cai-002");

        assertTrue(resultado.isEmpty());
    }
}
