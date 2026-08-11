/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.port.in.RegistrarTokenFcmPort;
import com.callsos.backend.infrastructure.config.CorsConfig;
import com.callsos.backend.infrastructure.config.SecurityConfig;
import com.callsos.backend.infrastructure.config.security.JwtAuthFilter;
import com.callsos.backend.infrastructure.config.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 4 (ruta técnica) — "Tests de Controllers REST: DenuncianteController".
 *
 * El foco principal es la validación de OWNERSHIP documentada en el
 * propio controller: hasRole("DENUNCIANTE") solo confirma que el caller
 * ES un denunciante, no que sea EL DUEÑO del recurso {id}. El controller
 * compara manualmente authentication.getName() contra el {id} del path.
 */
@WebMvcTest(DenuncianteController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("DenuncianteController")
class DenuncianteControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private RegistrarTokenFcmPort registrarTokenFcm;

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    @DisplayName("PATCH /{id}/token sin autenticación retorna 401")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(patch("/api/v1/denunciantes/den-001/token")
                .contentType("application/json")
                .content("{\"tokenFcm\": \"token-xyz\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /{id}/token con rol AGENTE retorna 403 (solo DENUNCIANTE)")
    void rolIncorrecto() throws Exception {
        mockMvc.perform(patch("/api/v1/denunciantes/den-001/token")
                .with(authentication(actor("ag-001", "AGENTE")))
                .contentType("application/json")
                .content("{\"tokenFcm\": \"token-xyz\"}"))
            .andExpect(status().isForbidden());

        verify(registrarTokenFcm, never()).ejecutar(any(), any());
    }

    @Test
    @DisplayName("PATCH /{id}/token con actorId distinto al {id} del path retorna 403 (ownership)")
    void ownershipViolado() throws Exception {
        // den-002 autenticado intentando modificar el token de den-001
        mockMvc.perform(patch("/api/v1/denunciantes/den-001/token")
                .with(authentication(actor("den-002", "DENUNCIANTE")))
                .contentType("application/json")
                .content("{\"tokenFcm\": \"token-xyz\"}"))
            .andExpect(status().isForbidden());

        verify(registrarTokenFcm, never()).ejecutar(any(), any());
    }

    @Test
    @DisplayName("PATCH /{id}/token con actorId igual al {id} del path retorna 204")
    void ownershipCorrecto() throws Exception {
        mockMvc.perform(patch("/api/v1/denunciantes/den-001/token")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content("{\"tokenFcm\": \"token-xyz\"}"))
            .andExpect(status().isNoContent());

        verify(registrarTokenFcm).ejecutar("den-001", "token-xyz");
    }

    @Test
    @DisplayName("PATCH /{id}/token con tokenFcm en blanco retorna 400 (validación)")
    void tokenFcmEnBlanco() throws Exception {
        mockMvc.perform(patch("/api/v1/denunciantes/den-001/token")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content("{\"tokenFcm\": \"\"}"))
            .andExpect(status().isBadRequest());
    }
}
