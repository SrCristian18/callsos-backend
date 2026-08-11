/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.in.ConsultarAgentesDisponiblesPorCaiPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
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
 * Épica 4 (ruta técnica) — GAP DETECTADO, no listado en la ruta original:
 * CaiController tampoco tenía test, aunque la Épica 4 solo mencionaba 5
 * controllers ("AuthController, IncidenteController, DenuncianteController,
 * ReporteController, AuditoriaController"). Se agrega junto con los demás.
 */
@WebMvcTest(CaiController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("CaiController")
class CaiControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private ConsultarAgentesDisponiblesPorCaiPort consultarDisponibles;

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    @DisplayName("GET /{caiId}/agentes/disponibles sin autenticación retorna 401")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/cais/cai-001/agentes/disponibles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{caiId}/agentes/disponibles con rol DENUNCIANTE retorna 403")
    void rolProhibido() throws Exception {
        mockMvc.perform(get("/api/v1/cais/cai-001/agentes/disponibles")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{caiId}/agentes/disponibles con rol OPERADOR_CAI retorna 200")
    void operadorCaiPuedeConsultar() throws Exception {
        Agente agente = new Agente(
            "ag-001", "Pedro", "Av. Test", new Ubicacion(10.4, -75.5), "3001111111");
        when(consultarDisponibles.ejecutar("cai-001")).thenReturn(List.of(agente));

        mockMvc.perform(get("/api/v1/cais/cai-001/agentes/disponibles")
                .with(authentication(actor("cai-001", "OPERADOR_CAI"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("ag-001"));
    }
}
