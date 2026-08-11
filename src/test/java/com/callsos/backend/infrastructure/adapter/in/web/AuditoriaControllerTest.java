/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 4 (ruta técnica) — "Tests de Controllers REST: AuditoriaController".
 */
@WebMvcTest(AuditoriaController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("AuditoriaController")
class AuditoriaControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private AuditoriaRepositoryPort auditoriaRepository;

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    @DisplayName("GET /incidente/{id} sin autenticación retorna 401")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /incidente/{id} con rol DENUNCIANTE retorna 403")
    void rolProhibido() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /incidente/{id} con rol OPERADOR_CAI retorna 200 con el historial")
    void operadorCaiPuedeConsultar() throws Exception {
        AuditoriaIncidente evento = new AuditoriaIncidente(
            "i-001", null, EstadoIncidente.CREADO, "den-001", "DENUNCIANTE", "Creado");
        when(auditoriaRepository.buscarPorIncidente("i-001")).thenReturn(List.of(evento));

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("cai-001", "OPERADOR_CAI"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].detalle").value("Creado"));
    }

    @Test
    @DisplayName("GET /incidente/{id} con rol COMANDO retorna 200")
    void comandoPuedeConsultar() throws Exception {
        when(auditoriaRepository.buscarPorIncidente("i-002")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-002")
                .with(authentication(actor("usr-comando", "COMANDO"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
