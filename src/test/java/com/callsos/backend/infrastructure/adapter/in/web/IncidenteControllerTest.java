/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.*;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.infrastructure.config.CorsConfig;
import com.callsos.backend.infrastructure.config.SecurityConfig;
import com.callsos.backend.infrastructure.config.security.JwtAuthFilter;
import com.callsos.backend.infrastructure.config.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 4 (ruta técnica) — "Tests de Controllers REST: IncidenteController".
 *
 * Usa authentication(...) de spring-security-test para inyectar
 * directamente un Authentication con rol en el SecurityContext, en vez
 * de generar JWTs reales — más simple y suficiente para probar el
 * contrato de autorización de SecurityConfig (que sí se importa real,
 * sin mocks, para no dar falsos positivos si sus reglas cambian).
 */
@WebMvcTest(IncidenteController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("IncidenteController")
class IncidenteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JwtService jwtService; // dependencia de JwtAuthFilter, no ejercitada directamente aquí
    @MockBean private CrearIncidentePort crearIncidente;
    @MockBean private CambiarEstadoIncidentePort cambiarEstado;
    @MockBean private ConsultarEstadoIncidentePort consultarEstado;
    @MockBean private ConsultarIncidentePort consultarIncidente;
    @MockBean private ConsultarMisIncidentesPort consultarMisIncidentes;
    @MockBean private ConsultarIncidentesAsignadosPort consultarAsignados;
    @MockBean private ConsultarIncidentesPorCAIPort consultarPorCAI;
    @MockBean private ConsultarIncidentesPorEstadoPort consultarPorEstado;
    // EPIC-18 (frontend) / hallazgo #14 — sin este @MockBean el
    // ApplicationContext de este slice test no carga (mismo motivo que
    // el FIX de simularRecorrido más abajo: el controller ahora
    // requiere este puerto en su constructor).
    @MockBean private ConsultarIncidentesDerivadosPort consultarDerivados;
    @MockBean private AsignarCAIAIncidentePort asignarCAI;
    @MockBean private AsignarAgentePort asignarAgente;
    @MockBean private MarcarAgenteEnCaminoPort marcarEnCamino;
    @MockBean private AtenderIncidentePort atenderIncidente;
    @MockBean private EvaluarIncidentePort evaluarIncidente;
    // FIX: IncidenteController requiere SimularRecorridoAgentePort en su
    // constructor (última dependencia, para el endpoint de simulación de
    // recorrido). Faltaba este @MockBean, por lo que Spring no podía
    // resolver el bean al construir el controller y el ApplicationContext
    // de este slice test fallaba al cargar — arrastrando los 16 tests
    // de esta clase con "ApplicationContext failure threshold exceeded".
    @MockBean private SimularRecorridoAgentePort simularRecorrido;
    @MockBean private ActualizarTipoIncidentePort actualizarTipo;
    @MockBean private ConsultarEtaPort consultarEta;
    @MockBean private com.callsos.backend.domain.port.out.AsignacionRepositoryPort asignacionRepository;

    private static UsernamePasswordAuthenticationToken actor(String id, String rol) {
        return new UsernamePasswordAuthenticationToken(
            id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Incidente incidenteDeEjemplo() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");
        return new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
    }

    // ── Autenticación / autorización básicas ────────────────────────────────

    @Test
    @DisplayName("GET /{id} sin autenticación retorna 401")
    void consultarSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/i-001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{id} autenticado (cualquier rol) retorna 200")
    void consultarAutenticado() throws Exception {
        when(consultarIncidente.ejecutar("i-001")).thenReturn(incidenteDeEjemplo());

        mockMvc.perform(get("/api/v1/incidentes/i-001")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("i-001"));
    }

    @Test
    @DisplayName("GET /{id} incidente inexistente retorna 404")
    void consultarInexistente() throws Exception {
        when(consultarIncidente.ejecutar("no-existe"))
            .thenThrow(new IllegalArgumentException("Incidente no encontrado"));

        mockMvc.perform(get("/api/v1/incidentes/no-existe")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isNotFound());
    }

    // ── Crear incidente ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST / con rol DENUNCIANTE retorna 201")
    void crearComoDenunciante() throws Exception {
        when(crearIncidente.ejecutar(eq("den-001"), eq(TipoIncidente.ROBOS_O_ASALTOS), any(), any()))
            .thenReturn(incidenteDeEjemplo());

        String body = """
            {
              "denuncianteId": "den-001",
              "tipo": "ROBOS_O_ASALTOS",
              "descripcion": "Robo en curso",
              "ubicacion": {"latitud": 10.4, "longitud": -75.5}
            }
            """;

        mockMvc.perform(post("/api/v1/incidentes")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST / con rol AGENTE retorna 403 (solo DENUNCIANTE puede crear)")
    void crearComoAgenteProhibido() throws Exception {
        String body = """
            {
              "denuncianteId": "den-001",
              "tipo": "ROBOS_O_ASALTOS",
              "descripcion": "Robo en curso",
              "ubicacion": {"latitud": 10.4, "longitud": -75.5}
            }
            """;

        mockMvc.perform(post("/api/v1/incidentes")
                .with(authentication(actor("ag-001", "AGENTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isForbidden());

        verifyNoInteractions(crearIncidente);
    }

    // ── Consultas propias del actor (mis-incidentes / asignados / por-cai) ──

    @Test
    @DisplayName("GET /mis-incidentes usa el actorId del JWT, no un parámetro del cliente")
    void misIncidentesUsaActorIdDelJwt() throws Exception {
        when(consultarMisIncidentes.ejecutar("den-001")).thenReturn(List.of(incidenteDeEjemplo()));

        mockMvc.perform(get("/api/v1/incidentes/mis-incidentes")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("i-001"));

        verify(consultarMisIncidentes).ejecutar("den-001");
    }

    @Test
    @DisplayName("GET /mis-incidentes con rol AGENTE retorna 403")
    void misIncidentesProhibidoParaAgente() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/mis-incidentes")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /asignados usa el actorId del JWT como agenteId")
    void asignadosUsaActorIdDelJwt() throws Exception {
        when(consultarAsignados.ejecutar("ag-001")).thenReturn(List.of(incidenteDeEjemplo()));

        mockMvc.perform(get("/api/v1/incidentes/asignados")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isOk());

        verify(consultarAsignados).ejecutar("ag-001");
    }

    @Test
    @DisplayName("GET /por-cai requiere OPERADOR_CAI o COMANDO")
    void porCaiRequiereRolCorrecto() throws Exception {
        when(consultarPorCAI.ejecutar("cai-001")).thenReturn(List.of(incidenteDeEjemplo()));

        mockMvc.perform(get("/api/v1/incidentes/por-cai")
                .with(authentication(actor("cai-001", "OPERADOR_CAI"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/incidentes/por-cai")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /por-estado filtra por el query param, visible para COMANDO")
    void porEstadoComando() throws Exception {
        when(consultarPorEstado.ejecutar(EstadoIncidente.CREADO))
            .thenReturn(List.of(incidenteDeEjemplo()));

        mockMvc.perform(get("/api/v1/incidentes/por-estado")
                .param("estado", "CREADO")
                .with(authentication(actor("usr-comando", "COMANDO"))))
            .andExpect(status().isOk());

        verify(consultarPorEstado).ejecutar(EstadoIncidente.CREADO);
    }

    @Test
    @DisplayName("GET /derivados devuelve el historial completo, visible para COMANDO")
    void derivadosComoComando() throws Exception {
        when(consultarDerivados.ejecutar()).thenReturn(List.of(incidenteDeEjemplo()));

        mockMvc.perform(get("/api/v1/incidentes/derivados")
                .with(authentication(actor("usr-comando", "COMANDO"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("i-001"));

        verify(consultarDerivados).ejecutar();
    }

    @Test
    @DisplayName("GET /derivados NO es visible para OPERADOR_CAI (a diferencia de /por-estado) ni para otros roles")
    void derivadosRechazaOtrosRoles() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/derivados")
                .with(authentication(actor("cai-001", "OPERADOR_CAI"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/incidentes/derivados")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/incidentes/derivados")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());

        verify(consultarDerivados, never()).ejecutar();
    }

    @Test
    @DisplayName("GET /derivados sin autenticación retorna 401")
    void derivadosSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/derivados"))
            .andExpect(status().isUnauthorized());
    }

    // ── Mutaciones de estado ──────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/derivar con rol COMANDO retorna 204")
    void derivarComoComando() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/derivar")
                .with(authentication(actor("usr-comando", "COMANDO"))))
            .andExpect(status().isNoContent());

        verify(asignarCAI).ejecutar("i-001");
    }

    @Test
    @DisplayName("PATCH /{id}/atender con rol AGENTE retorna 204 y usa el actorId del JWT")
    void atenderComoAgente() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/atender")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isNoContent());

        verify(atenderIncidente).ejecutar("i-001", "ag-001");
    }

    @Test
    @DisplayName("PATCH /{id}/atender con rol DENUNCIANTE retorna 403")
    void atenderComoDenuncianteProhibido() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/atender")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(atenderIncidente);
    }

    @Test
    @DisplayName("PATCH /{id}/atender con transición inválida retorna 422")
    void atenderTransicionInvalida() throws Exception {
        doThrow(new IllegalStateException("Transición inválida"))
            .when(atenderIncidente).ejecutar("i-001", "ag-001");

        mockMvc.perform(patch("/api/v1/incidentes/i-001/atender")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isUnprocessableEntity());
    }

    // ── Ownership entre agentes — Épica 8, hallazgo de seguridad #2 ─────────

    @Test
    @DisplayName("PATCH /{id}/en-camino: agente ajeno (no asignado) retorna 403")
    void enCaminoAgenteAjenoRetorna403() throws Exception {
        doThrow(new AccesoDenegadoException(
                "El agente autenticado no es el agente asignado a este incidente."))
            .when(marcarEnCamino).ejecutar("i-001", "ag-002");

        mockMvc.perform(patch("/api/v1/incidentes/i-001/en-camino")
                .with(authentication(actor("ag-002", "AGENTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/atender: agente ajeno (no asignado) retorna 403")
    void atenderAgenteAjenoRetorna403() throws Exception {
        doThrow(new AccesoDenegadoException(
                "El agente autenticado no es el agente asignado a este incidente."))
            .when(atenderIncidente).ejecutar("i-001", "ag-002");

        mockMvc.perform(patch("/api/v1/incidentes/i-001/atender")
                .with(authentication(actor("ag-002", "AGENTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/evaluar: agente ajeno (no asignado) retorna 403")
    void evaluarAgenteAjenoRetorna403() throws Exception {
        doThrow(new AccesoDenegadoException(
                "El agente autenticado no es el agente asignado a este incidente."))
            .when(evaluarIncidente).ejecutar("i-001", "ag-002");

        mockMvc.perform(patch("/api/v1/incidentes/i-001/evaluar")
                .with(authentication(actor("ag-002", "AGENTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/evaluar con rol AGENTE (dueño) retorna 204 y usa el actorId del JWT")
    void evaluarComoAgenteDueno() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/evaluar")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isNoContent());

        verify(evaluarIncidente).ejecutar("i-001", "ag-001");
    }

    @Test
    @DisplayName("PATCH /{id}/en-camino con rol AGENTE (dueño) retorna 204 y usa el actorId del JWT")
    void enCaminoComoAgenteDueno() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/en-camino")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isNoContent());

        verify(marcarEnCamino).ejecutar("i-001", "ag-001");
    }

    @Test
    @DisplayName("PATCH /{id}/cancelar acepta DENUNCIANTE o COMANDO")
    void cancelarPermiteDenuncianteYComando() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/cancelar")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isNoContent());

        verify(cambiarEstado).ejecutar("i-001", EstadoIncidente.CANCELADO);
    }

    @Test
    @DisplayName("PATCH /{id}/cancelar con rol AGENTE retorna 403")
    void cancelarProhibidoParaAgente() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/cancelar")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isForbidden());
    }

    // ── Actualizar tipo de incidente — Épica 1 ──────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/tipo con rol DENUNCIANTE (dueño) retorna 204")
    void actualizarTipoComoDenuncianteDueno() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isNoContent());

        verify(actualizarTipo).ejecutar("i-001", "den-001", TipoIncidente.RIÑAS_O_PELEAS);
    }

    @Test
    @DisplayName("PATCH /{id}/tipo con rol AGENTE retorna 403 (solo DENUNCIANTE)")
    void actualizarTipoProhibidoParaAgente() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("ag-001", "AGENTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isForbidden());

        verifyNoInteractions(actualizarTipo);
    }

    @Test
    @DisplayName("PATCH /{id}/tipo con rol COMANDO retorna 403 (esta acción es solo del denunciante)")
    void actualizarTipoProhibidoParaComando() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("usr-comando", "COMANDO")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isForbidden());

        verifyNoInteractions(actualizarTipo);
    }

    @Test
    @DisplayName("PATCH /{id}/tipo sin autenticación retorna 401")
    void actualizarTipoSinAutenticacion() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /{id}/tipo con body sin nuevoTipo retorna 400")
    void actualizarTipoSinCuerpoValido() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarTipo);
    }

    @Test
    @DisplayName("PATCH /{id}/tipo sobre incidente de otro denunciante retorna 403 (ownership)")
    void actualizarTipoIncidenteAjenoRetorna403() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;
        doThrow(new AccesoDenegadoException(
                "El denunciante autenticado no es el dueño de este incidente."))
            .when(actualizarTipo).ejecutar("i-001", "den-999", TipoIncidente.RIÑAS_O_PELEAS);

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("den-999", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/tipo sobre incidente cerrado retorna 422")
    void actualizarTipoIncidenteCerradoRetorna422() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;
        doThrow(new IllegalStateException("No se puede cambiar el tipo de un incidente FINALIZADO."))
            .when(actualizarTipo).ejecutar("i-001", "den-001", TipoIncidente.RIÑAS_O_PELEAS);

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /{id}/tipo con el mismo tipo actual retorna 422")
    void actualizarMismoTipoRetorna422() throws Exception {
        String body = """
            { "nuevoTipo": "ROBOS_O_ASALTOS" }
            """;
        doThrow(new IllegalStateException("El incidente ya tiene el tipo ROBOS_O_ASALTOS."))
            .when(actualizarTipo).ejecutar("i-001", "den-001", TipoIncidente.ROBOS_O_ASALTOS);

        mockMvc.perform(patch("/api/v1/incidentes/i-001/tipo")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /{id}/tipo sobre incidente inexistente retorna 404")
    void actualizarTipoIncidenteInexistenteRetorna404() throws Exception {
        String body = """
            { "nuevoTipo": "RIÑAS_O_PELEAS" }
            """;
        doThrow(new IllegalArgumentException("Incidente no encontrado: no-existe"))
            .when(actualizarTipo).ejecutar("no-existe", "den-001", TipoIncidente.RIÑAS_O_PELEAS);

        mockMvc.perform(patch("/api/v1/incidentes/no-existe/tipo")
                .with(authentication(actor("den-001", "DENUNCIANTE")))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isNotFound());
    }

    // ── ETA — Épica 4 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id}/eta con rol DENUNCIANTE (dueño) y datos disponibles retorna 200 con minutos y categoría")
    void etaComoDenuncianteDuenoConDatos() throws Exception {
        com.callsos.backend.domain.model.EtaInfo eta =
            com.callsos.backend.domain.model.EtaInfo.calcular(556.0, 36.0); // ~1 min, MENOS_DE_1_KM
        when(consultarEta.consultar("i-001", "den-001")).thenReturn(eta);

        mockMvc.perform(get("/api/v1/incidentes/i-001/eta")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minutosEstimados").value(eta.getMinutosEstimados()))
            .andExpect(jsonPath("$.categoriaDistancia").value("MENOS_DE_1_KM"));
    }

    @Test
    @DisplayName("GET /{id}/eta sin datos disponibles (agente aún no reporta posición) retorna 200 con valores null, no error")
    void etaSinDatosDisponibles() throws Exception {
        when(consultarEta.consultar("i-001", "den-001"))
            .thenReturn(com.callsos.backend.domain.model.EtaInfo.sinDatos());

        mockMvc.perform(get("/api/v1/incidentes/i-001/eta")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minutosEstimados").doesNotExist())
            .andExpect(jsonPath("$.categoriaDistancia").doesNotExist());
    }

    @Test
    @DisplayName("GET /{id}/eta con rol AGENTE retorna 403 (solo DENUNCIANTE puede consultar su ETA)")
    void etaProhibidaParaAgente() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/i-001/eta")
                .with(authentication(actor("ag-001", "AGENTE"))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(consultarEta);
    }

    @Test
    @DisplayName("GET /{id}/eta con rol COMANDO retorna 403 (solo DENUNCIANTE, ver SecurityConfig)")
    void etaProhibidaParaComando() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/i-001/eta")
                .with(authentication(actor("usr-comando", "COMANDO"))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(consultarEta);
    }

    @Test
    @DisplayName("GET /{id}/eta sin autenticación retorna 401")
    void etaSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/i-001/eta"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{id}/eta sobre incidente de otro denunciante retorna 403 (ownership)")
    void etaIncidenteAjenoRetorna403() throws Exception {
        doThrow(new AccesoDenegadoException(
                "El denunciante autenticado no es el dueño de este incidente."))
            .when(consultarEta).consultar("i-001", "den-999");

        mockMvc.perform(get("/api/v1/incidentes/i-001/eta")
                .with(authentication(actor("den-999", "DENUNCIANTE"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id}/eta sobre incidente inexistente retorna 404")
    void etaIncidenteInexistenteRetorna404() throws Exception {
        doThrow(new IllegalArgumentException("Incidente no encontrado: no-existe"))
            .when(consultarEta).consultar("no-existe", "den-001");

        mockMvc.perform(get("/api/v1/incidentes/no-existe/eta")
                .with(authentication(actor("den-001", "DENUNCIANTE"))))
            .andExpect(status().isNotFound());
    }
}