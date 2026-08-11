/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteAdministrativo;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.in.CrearReporteAdministrativoPort;
import com.callsos.backend.domain.port.in.CrearReporteHallazgosPort;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 4 (ruta técnica) — "Tests de Controllers REST: ReporteController".
 *
 * Los DTOs de request aquí son records de Java (ver ReporteController),
 * a diferencia de AuthController/IncidenteController que usan clases con
 * campos privados sin setters — por eso este test no comparte el mismo
 * riesgo de deserialización señalado en AuthControllerTest.
 */
@WebMvcTest(ReporteController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("ReporteController")
class ReporteControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private CrearReporteHallazgosPort crearHallazgos;
    @MockBean private CrearReporteAdministrativoPort crearAdministrativo;

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
    private final Ubicacion ubicacion = new Ubicacion(10.4, -75.5);

    @Test
    @DisplayName("POST /hallazgos con rol AGENTE retorna 201")
    void crearHallazgosComoAgente() throws Exception {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        Agente agente = new Agente("ag-001", "Pedro", "Av. Test", ubicacion, "300");
        when(crearHallazgos.ejecutar("i-001", "ag-001", "Hallazgo importante"))
            .thenReturn(new ReporteHallazgos("rh-001", "Hallazgo importante", incidente, agente));

        String body = """
            {"incidenteId": "i-001", "agenteId": "ag-001", "descripcion": "Hallazgo importante"}
            """;

        mockMvc.perform(post("/api/v1/reportes/hallazgos")
                .with(authentication(actor("ag-001", "AGENTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /hallazgos con rol DENUNCIANTE retorna 403")
    void crearHallazgosProhibidoParaDenunciante() throws Exception {
        String body = """
            {"incidenteId": "i-001", "agenteId": "ag-001", "descripcion": "Hallazgo importante"}
            """;

        mockMvc.perform(post("/api/v1/reportes/hallazgos")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isForbidden());

        verifyNoInteractions(crearHallazgos);
    }

    @Test
    @DisplayName("POST /hallazgos con descripción en blanco retorna 400")
    void crearHallazgosDescripcionEnBlanco() throws Exception {
        String body = """
            {"incidenteId": "i-001", "agenteId": "ag-001", "descripcion": ""}
            """;

        mockMvc.perform(post("/api/v1/reportes/hallazgos")
                .with(authentication(actor("ag-001", "AGENTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /administrativo con rol OPERADOR_CAI retorna 201")
    void crearAdministrativoComoOperador() throws Exception {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denunciante);
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga", "Calle 1", ubicacion, "601");
        when(crearAdministrativo.ejecutar("i-001", "cai-001", "Resumen del caso"))
            .thenReturn(new ReporteAdministrativo("ra-001", "Resumen del caso", incidente, cai));

        String body = """
            {"incidenteId": "i-001", "autoridadId": "cai-001", "resumen": "Resumen del caso"}
            """;

        mockMvc.perform(post("/api/v1/reportes/administrativo")
                .with(authentication(actor("cai-001", "OPERADOR_CAI")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /administrativo con rol AGENTE retorna 403")
    void crearAdministrativoProhibidoParaAgente() throws Exception {
        String body = """
            {"incidenteId": "i-001", "autoridadId": "cai-001", "resumen": "Resumen del caso"}
            """;

        mockMvc.perform(post("/api/v1/reportes/administrativo")
                .with(authentication(actor("ag-001", "AGENTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isForbidden());

        verifyNoInteractions(crearAdministrativo);
    }
}
