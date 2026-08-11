/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarTokenFcmService")
class RegistrarTokenFcmServiceTest {

    @Mock DenuncianteRepositoryPort denuncianteRepository;

    RegistrarTokenFcmService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarTokenFcmService(denuncianteRepository);
    }

    @Test
    @DisplayName("denunciante existente actualiza el token FCM")
    void actualizaTokenFcm() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));

        service.ejecutar("den-001", "nuevo-token-fcm-xyz");

        verify(denuncianteRepository).actualizarTokenFcm("den-001", "nuevo-token-fcm-xyz");
    }

    @Test
    @DisplayName("denunciante inexistente lanza IllegalArgumentException y no actualiza")
    void denuncianteNoEncontrado() {
        when(denuncianteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.ejecutar("no-existe", "token-xyz"));

        verify(denuncianteRepository, never()).actualizarTokenFcm(any(), any());
    }
}
