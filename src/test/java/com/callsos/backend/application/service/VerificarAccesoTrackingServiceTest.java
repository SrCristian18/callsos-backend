/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Cubre VerificarAccesoTrackingService — Épica 3 (fix P6), la regla de
 * seguridad más crítica del sistema: el denunciante nunca puede ver la
 * ubicación del agente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificarAccesoTrackingService — Épica 3")
class VerificarAccesoTrackingServiceTest {

    @Mock AgenteRepositoryPort agenteRepository;

    VerificarAccesoTrackingService service;

    @BeforeEach
    void setUp() {
        service = new VerificarAccesoTrackingService(agenteRepository);
    }

    @Test
    @DisplayName("AGENTE puede ver su propio tracking")
    void agentePropioAutorizado() {
        assertTrue(service.puedeAcceder("ag-001", "ag-001", "AGENTE"));
    }

    @Test
    @DisplayName("AGENTE NO puede ver el tracking de otro agente")
    void agenteAjenoRechazado() {
        assertFalse(service.puedeAcceder("ag-001", "ag-999", "AGENTE"));
        verifyNoInteractions(agenteRepository);
    }

    @Test
    @DisplayName("OPERADOR_CAI puede ver el tracking de un agente de su propia unidad")
    void caiDeSuPropiaUnidadAutorizado() {
        when(agenteRepository.buscarUnidadDeAgente("ag-001"))
            .thenReturn(Optional.of("cai-001"));

        assertTrue(service.puedeAcceder("ag-001", "cai-001", "OPERADOR_CAI"));
    }

    @Test
    @DisplayName("OPERADOR_CAI NO puede ver el tracking de un agente de OTRA unidad")
    void caiDeOtraUnidadRechazado() {
        when(agenteRepository.buscarUnidadDeAgente("ag-001"))
            .thenReturn(Optional.of("cai-999"));

        assertFalse(service.puedeAcceder("ag-001", "cai-001", "OPERADOR_CAI"));
    }

    @Test
    @DisplayName("OPERADOR_CAI rechazado si el agente no existe (unidad no resuelve)")
    void caiConAgenteInexistenteRechazado() {
        when(agenteRepository.buscarUnidadDeAgente("ag-fantasma"))
            .thenReturn(Optional.empty());

        assertFalse(service.puedeAcceder("ag-fantasma", "cai-001", "OPERADOR_CAI"));
    }

    @Test
    @DisplayName("COMANDO puede ver el tracking de cualquier agente, sin consultar el repositorio")
    void comandoSiempreAutorizado() {
        assertTrue(service.puedeAcceder("ag-001", "usr-comando", "COMANDO"));
        assertTrue(service.puedeAcceder("ag-999", "usr-comando", "COMANDO"));
        verifyNoInteractions(agenteRepository);
    }

    @Test
    @DisplayName("DENUNCIANTE SIEMPRE rechazado — regla de seguridad central del sistema")
    void denuncianteSiempreRechazado() {
        assertFalse(service.puedeAcceder("ag-001", "den-001", "DENUNCIANTE"));
        // Incluso si por alguna coincidencia el ID del denunciante fuera
        // igual al del agente consultado, sigue rechazado: la condición
        // de "AGENTE" solo aplica cuando el rol real es AGENTE.
        assertFalse(service.puedeAcceder("ag-001", "ag-001", "DENUNCIANTE"));
        verifyNoInteractions(agenteRepository);
    }

    @Test
    @DisplayName("rol desconocido o vacío rechazado por defecto (fail-closed)")
    void rolDesconocidoRechazado() {
        assertFalse(service.puedeAcceder("ag-001", "x-001", "ROL_INEXISTENTE"));
        assertFalse(service.puedeAcceder("ag-001", "x-001", ""));
    }

    @Test
    @DisplayName("parámetros null rechazados sin lanzar excepción")
    void parametrosNulosRechazados() {
        assertFalse(service.puedeAcceder(null, "x-001", "AGENTE"));
        assertFalse(service.puedeAcceder("ag-001", null, "AGENTE"));
        assertFalse(service.puedeAcceder("ag-001", "x-001", null));
    }
}