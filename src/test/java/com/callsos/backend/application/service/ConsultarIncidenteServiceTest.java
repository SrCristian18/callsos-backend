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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarIncidenteService")
class ConsultarIncidenteServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;

    ConsultarIncidenteService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarIncidenteService(incidenteRepository);
    }

    @Test
    @DisplayName("retorna el incidente completo cuando existe")
    void retornaIncidente() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        Incidente resultado = service.ejecutar("i-001");

        assertSame(incidente, resultado);
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar("no-existe"));
    }
}
