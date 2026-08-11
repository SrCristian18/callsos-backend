/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.out.InvitacionAgenteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerarInvitacionAgenteService")
class GenerarInvitacionAgenteServiceTest {

    @Mock InvitacionAgenteRepositoryPort invitacionRepository;

    GenerarInvitacionAgenteService service;

    @BeforeEach
    void setUp() {
        service = new GenerarInvitacionAgenteService(invitacionRepository);
    }

    @Test
    @DisplayName("genera una invitación vigente, sin usar, y la guarda")
    void generaInvitacion() {
        InvitacionAgente invitacion = service.ejecutar("cai-001", "usr-comando-001");

        assertNotNull(invitacion.getToken());
        assertFalse(invitacion.getToken().isBlank());
        assertEquals("cai-001", invitacion.getUnidadPolicialId());
        assertEquals("usr-comando-001", invitacion.getCreadoPor());
        assertFalse(invitacion.isUsado());
        assertTrue(invitacion.estaVigente());

        ArgumentCaptor<InvitacionAgente> captor = ArgumentCaptor.forClass(InvitacionAgente.class);
        verify(invitacionRepository).guardar(captor.capture());
        assertEquals(invitacion.getToken(), captor.getValue().getToken());
    }

    @Test
    @DisplayName("dos invitaciones generadas tienen tokens distintos")
    void tokensUnicos() {
        InvitacionAgente primera = service.ejecutar("cai-001", "usr-comando-001");
        InvitacionAgente segunda = service.ejecutar("cai-001", "usr-comando-001");

        assertNotEquals(primera.getToken(), segunda.getToken());
    }
}
