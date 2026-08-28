package com.callsos.backend.application.service;

import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Épica 5 — mismo patrón que RegistrarTokenFcmServiceTest (denunciante).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarTokenFcmAgenteService")
class RegistrarTokenFcmAgenteServiceTest {

    @Mock AgenteRepositoryPort agenteRepository;

    RegistrarTokenFcmAgenteService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarTokenFcmAgenteService(agenteRepository);
    }

    @Test
    @DisplayName("agente existente actualiza el token FCM")
    void actualizaTokenFcm() {
        when(agenteRepository.buscarUnidadDeAgente("ag-001"))
            .thenReturn(Optional.of("cai-001"));

        service.ejecutar("ag-001", "nuevo-token-fcm-xyz");

        verify(agenteRepository).actualizarTokenFcm("ag-001", "nuevo-token-fcm-xyz");
    }

    @Test
    @DisplayName("agente inexistente lanza IllegalArgumentException y no actualiza")
    void agenteNoEncontrado() {
        when(agenteRepository.buscarUnidadDeAgente("no-existe"))
            .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "token-xyz"));

        verify(agenteRepository, never()).actualizarTokenFcm(any(), any());
    }
}
