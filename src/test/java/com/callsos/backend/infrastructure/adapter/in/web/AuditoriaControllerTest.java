/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 2 (fix P7 / Regla 5) — el filtrado por actor ahora vive en el
 * controller, no en SecurityConfig, así que estos tests reemplazan por
 * completo el "rolProhibido" de la Épica 4 (que asumía que DENUNCIANTE
 * jamás podía consultar auditoría — eso ya no es cierto: SÍ puede, pero
 * solo la de SUS PROPIOS incidentes).
 */
@WebMvcTest(AuditoriaController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("AuditoriaController")
class AuditoriaControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private AuditoriaRepositoryPort auditoriaRepository;
    @MockBean private IncidenteRepositoryPort incidenteRepository;

    private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
    private final Denunciante denuncianteDueno = new Denunciante(
        "den-001", "Juan", "Cartagena", "300", "j@test.com");

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Incidente incidenteBase() {
        return new Incidente("i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            ubicacion, denuncianteDueno);
    }

    @Test
    @DisplayName("GET /incidente/{id} sin autenticación retorna 401")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DENUNCIANTE dueño del incidente recibe 200 (Épica 2)")
    void denuncianteDuenoPuedeConsultar() throws Exception {
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidenteBase()));
        when(auditoriaRepository.buscarPorIncidente("i-001")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DENUNCIANTE ajeno (no dueño) recibe 403 (fix P7 / Regla 5)")
    void denuncianteAjenoRechazado() throws Exception {
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidenteBase()));

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("den-999", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AGENTE asignado al incidente recibe 200")
    void agenteAsignadoPuedeConsultar() throws Exception {
        Incidente incidente = incidenteBase();
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga", "Dir", ubicacion, "600");
        incidente.derivarACAI(cai);
        Agente agente = new Agente("ag-001", "Pedro", "Dir", ubicacion, "300");
        com.callsos.backend.domain.model.Denuncia denuncia = new com.callsos.backend.domain.model.Denuncia(
            "den-audit-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacion, denuncianteDueno, incidente);
        incidente.setDenuncia(denuncia);
        incidente.agregarAsignacion(new Asignacion("asig-001", agente, denuncia));

        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(auditoriaRepository.buscarPorIncidente("i-001")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AGENTE no asignado al incidente recibe 403")
    void agenteNoAsignadoRechazado() throws Exception {
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidenteBase()));

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("ag-999", "AGENTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPERADOR_CAI de la unidad propia del incidente retorna 200 con el historial")
    void operadorCaiPropioPuedeConsultar() throws Exception {
        Incidente incidente = incidenteBase();
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga", "Dir", ubicacion, "600");
        incidente.derivarACAI(cai);

        AuditoriaIncidente evento = new AuditoriaIncidente(
            "i-001", null, EstadoIncidente.CREADO, "den-001", "DENUNCIANTE", "Creado");

        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(auditoriaRepository.buscarPorIncidente("i-001")).thenReturn(List.of(evento));

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("cai-001", "OPERADOR_CAI"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].detalle").value("Creado"));
    }

    @Test
    @DisplayName("OPERADOR_CAI de OTRA unidad (no la del incidente) recibe 403 (fix P7 / Regla 5)")
    void operadorCaiAjenoRechazado() throws Exception {
        Incidente incidente = incidenteBase();
        UnidadPolicial cai = new UnidadPolicial("cai-001", "CAI Manga", "Dir", ubicacion, "600");
        incidente.derivarACAI(cai);

        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("cai-999", "OPERADOR_CAI"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPERADOR_CAI cuando el incidente aún no tiene CAI asignado recibe 403")
    void operadorCaiSinUnidadAsignadaRechazado() throws Exception {
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidenteBase()));

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-001")
                .with(authentication(actor("cai-001", "OPERADOR_CAI"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("COMANDO tiene acceso global, sin necesidad de buscar el incidente")
    void comandoPuedeConsultar() throws Exception {
        when(auditoriaRepository.buscarPorIncidente("i-002")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/auditoria/incidente/i-002")
                .with(authentication(actor("usr-comando", "COMANDO"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
