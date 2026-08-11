/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.in.GenerarInvitacionAgentePort;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 4 (ruta técnica) — GAP DETECTADO, no listado en la ruta original:
 * InvitacionController tampoco tenía test. Se agrega junto con los demás.
 *
 * NOTA — mismo riesgo de deserialización que en AuthControllerTest:
 * GenerarInvitacionRequest es una clase con campo privado y SIN setter.
 * Si "generaInvitacionComoComando" falla con 400 en vez de 200, es la
 * misma causa raíz señalada allá.
 */
@WebMvcTest(InvitacionController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("InvitacionController")
class InvitacionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private GenerarInvitacionAgentePort generarInvitacionPort;

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    @DisplayName("POST / sin autenticación retorna 401")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(post("/api/v1/invitaciones")
                .contentType("application/json")
                .content("{\"unidadPolicialId\": \"cai-001\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST / con rol distinto de COMANDO retorna 403")
    void rolProhibido() throws Exception {
        mockMvc.perform(post("/api/v1/invitaciones")
                .with(authentication(actor("cai-001", "OPERADOR_CAI")))
                .contentType("application/json")
                .content("{\"unidadPolicialId\": \"cai-001\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST / con rol COMANDO retorna 200, usando el actorId del JWT como creadoPor")
    void generaInvitacionComoComando() throws Exception {
        InvitacionAgente invitacion = InvitacionAgente.generar("cai-001", "usr-comando-001");
        when(generarInvitacionPort.ejecutar(eq("cai-001"), eq("usr-comando-001")))
            .thenReturn(invitacion);

        mockMvc.perform(post("/api/v1/invitaciones")
                .with(authentication(actor("usr-comando-001", "COMANDO")))
                .contentType("application/json")
                .content("{\"unidadPolicialId\": \"cai-001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unidadPolicialId").value("cai-001"));

        // El creadoPor viene del JWT, nunca de un campo que el cliente pudiera enviar en el body
        verify(generarInvitacionPort).ejecutar("cai-001", "usr-comando-001");
    }
}
