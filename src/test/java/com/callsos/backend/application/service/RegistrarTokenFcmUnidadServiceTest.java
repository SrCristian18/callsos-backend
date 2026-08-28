package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
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
 * Épica 5 — mismo patrón que RegistrarTokenFcmServiceTest (denunciante)
 * y RegistrarTokenFcmAgenteServiceTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarTokenFcmUnidadService")
class RegistrarTokenFcmUnidadServiceTest {

    @Mock UnidadPolicialRepositoryPort unidadPolicialRepository;

    RegistrarTokenFcmUnidadService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarTokenFcmUnidadService(unidadPolicialRepository);
    }

    @Test
    @DisplayName("unidad existente actualiza el token FCM")
    void actualizaTokenFcm() {
        UnidadPolicial cai = new UnidadPolicial(
            "cai-001", "CAI Test", "Calle Test", new Ubicacion(10.4, -75.5), "6010000");
        when(unidadPolicialRepository.buscarPorId("cai-001")).thenReturn(Optional.of(cai));

        service.ejecutar("cai-001", "nuevo-token-fcm-xyz");

        verify(unidadPolicialRepository).actualizarTokenFcm("cai-001", "nuevo-token-fcm-xyz");
    }

    @Test
    @DisplayName("unidad inexistente lanza IllegalArgumentException y no actualiza")
    void unidadNoEncontrada() {
        when(unidadPolicialRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "token-xyz"));

        verify(unidadPolicialRepository, never()).actualizarTokenFcm(any(), any());
    }
}
