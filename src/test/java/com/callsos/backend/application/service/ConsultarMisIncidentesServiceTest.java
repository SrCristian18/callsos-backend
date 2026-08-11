/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

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
@DisplayName("ConsultarMisIncidentesService")
class ConsultarMisIncidentesServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;

    ConsultarMisIncidentesService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarMisIncidentesService(incidenteRepository);
    }

    @Test
    @DisplayName("delega en incidenteRepository.buscarPorDenunciante")
    void delegaCorrectamente() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        when(incidenteRepository.buscarPorDenunciante("den-001"))
            .thenReturn(List.of(incidente));

        List<Incidente> resultado = service.ejecutar("den-001");

        assertEquals(1, resultado.size());
        verify(incidenteRepository).buscarPorDenunciante("den-001");
    }

    @Test
    @DisplayName("retorna lista vacía si el denunciante no tiene historial")
    void listaVacia() {
        when(incidenteRepository.buscarPorDenunciante("den-002")).thenReturn(List.of());

        List<Incidente> resultado = service.ejecutar("den-002");

        assertTrue(resultado.isEmpty());
    }
}
