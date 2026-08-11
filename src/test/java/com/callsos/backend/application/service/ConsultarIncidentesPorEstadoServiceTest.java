/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarIncidentesPorEstadoService")
class ConsultarIncidentesPorEstadoServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;

    ConsultarIncidentesPorEstadoService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarIncidentesPorEstadoService(incidenteRepository);
    }

    @Test
    @DisplayName("delega en incidenteRepository.buscarPorEstado — sin restricción de actor (visibilidad total de Comando)")
    void delegaCorrectamente() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        when(incidenteRepository.buscarPorEstado(EstadoIncidente.CREADO))
            .thenReturn(List.of(incidente));

        List<Incidente> resultado = service.ejecutar(EstadoIncidente.CREADO);

        assertEquals(1, resultado.size());
        verify(incidenteRepository).buscarPorEstado(EstadoIncidente.CREADO);
    }

    @Test
    @DisplayName("retorna lista vacía si no hay incidentes en ese estado")
    void listaVacia() {
        when(incidenteRepository.buscarPorEstado(EstadoIncidente.FINALIZADO))
            .thenReturn(List.of());

        List<Incidente> resultado = service.ejecutar(EstadoIncidente.FINALIZADO);

        assertTrue(resultado.isEmpty());
    }
}
