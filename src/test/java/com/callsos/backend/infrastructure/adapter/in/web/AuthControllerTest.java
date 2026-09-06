/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.domain.port.in.RegistrarAgenteConInvitacionPort;
import com.callsos.backend.domain.port.in.RegistrarDenunciantePort;
import com.callsos.backend.domain.port.in.ResetearPasswordPort;
import com.callsos.backend.domain.port.in.SolicitarReseteoPasswordPort;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Épica 4 (ruta técnica) — "Tests de Controllers REST: AuthController".
 *
 * /api/v1/auth/** es público (permitAll en SecurityConfig) — no requiere
 * Authorization header. Se importa igual toda la cadena de seguridad
 * real (SecurityConfig + CorsConfig + JwtAuthFilter) para verificar que
 * efectivamente NO exige JWT en estas rutas, no para usarlo.
 *
 * NOTA — riesgo detectado durante la escritura de este test (Épica 4):
 * AuthRequest, RegistroDenuncianteRequest y RegistroAgenteRequest son
 * clases con campos privados, SIN setters y SIN constructor explícito.
 * Jackson, por defecto, solo detecta setters o campos públicos para
 * deserializar — sin uno de los dos, el JSON entrante podría llegar
 * como un objeto con todos los campos en null, y @Valid rechazaría
 * SIEMPRE con 400 sin importar qué envíe el cliente. Si el test
 * "loginExitoso" de abajo falla con 400 en vez de 200, es casi
 * seguro esta causa — la corrección sería agregar @Setter (Lombok,
 * ya está en el pom) o setters explícitos a esas 3 clases.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthFilter.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JwtService jwtService; // dependencia de JwtAuthFilter, no se usa en estas rutas públicas
    @MockBean private LoginPort loginPort;
    @MockBean private RegistrarDenunciantePort registrarDenunciantePort;
    @MockBean private RegistrarAgenteConInvitacionPort registrarAgentePort;
    @MockBean private SolicitarReseteoPasswordPort solicitarReseteoPort;
    @MockBean private ResetearPasswordPort resetearPasswordPort;

    @Test
    @DisplayName("POST /login exitoso retorna 200 con el token y los datos del actor")
    void loginExitoso() throws Exception {
        when(loginPort.ejecutar("juan.denunciante", "password123"))
            .thenReturn(new LoginPort.LoginResultado(
                "jwt-token-xyz", "den-001", "DENUNCIANTE", "Juan Test"));

        String body = """
            {"username": "juan.denunciante", "password": "password123"}
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token-xyz"))
            .andExpect(jsonPath("$.actorId").value("den-001"))
            .andExpect(jsonPath("$.rol").value("DENUNCIANTE"));
    }

    @Test
    @DisplayName("POST /login con credenciales inválidas retorna 404 (IllegalArgumentException)")
    void loginCredencialesInvalidas() throws Exception {
        when(loginPort.ejecutar(any(), any()))
            .thenThrow(new IllegalArgumentException("Usuario o contraseña incorrectos"));

        String body = """
            {"username": "no.existe", "password": "loquesea"}
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /login sin username retorna 400 (validación)")
    void loginSinUsername() throws Exception {
        String body = """
            {"username": "", "password": "password123"}
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /registro/denunciante exitoso retorna 200 con autologueo")
    void registroDenuncianteExitoso() throws Exception {
        when(registrarDenunciantePort.ejecutar(any()))
            .thenReturn(new LoginPort.LoginResultado(
                "jwt-token-nuevo", "den-002", "DENUNCIANTE", "Ana Nueva"));

        String body = """
            {
              "nombre": "Ana",
              "apellido": "Nueva",
              "documento": "1009999999",
              "telefono": "3009999999",
              "correo": "ana.nueva@callsos.test",
              "password": "Password123",
              "confirmarPassword": "Password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/denunciante")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actorId").value("den-002"));
    }

    @Test
    @DisplayName("POST /registro/denunciante con regla de negocio violada retorna 422")
    void registroDenuncianteReglaDeNegocio() throws Exception {
        when(registrarDenunciantePort.ejecutar(any()))
            .thenThrow(new IllegalStateException("Las contraseñas no coinciden"));

        String body = """
            {
              "nombre": "Ana",
              "apellido": "Nueva",
              "documento": "1009999999",
              "telefono": "3009999999",
              "correo": "ana.nueva@callsos.test",
              "password": "Password123",
              "confirmarPassword": "OtraCosa456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/denunciante")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /registro/denunciante sin correo (Épica 8, hallazgo #6, Parte 1) retorna 400")
    void registroDenuncianteSinCorreo() throws Exception {
        String body = """
            {
              "nombre": "Ana",
              "apellido": "Nueva",
              "documento": "1009999999",
              "telefono": "3009999999",
              "password": "Password123",
              "confirmarPassword": "Password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/denunciante")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registrarDenunciantePort);
    }

    @Test
    @DisplayName("POST /registro/denunciante con correo con formato inválido retorna 400")
    void registroDenuncianteCorreoInvalido() throws Exception {
        String body = """
            {
              "nombre": "Ana",
              "apellido": "Nueva",
              "documento": "1009999999",
              "telefono": "3009999999",
              "correo": "esto-no-es-un-correo",
              "password": "Password123",
              "confirmarPassword": "Password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/denunciante")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registrarDenunciantePort);
    }

    @Test
    @DisplayName("POST /registro/agente exitoso retorna 200 con autologueo")
    void registroAgenteExitoso() throws Exception {
        when(registrarAgentePort.ejecutar(any()))
            .thenReturn(new LoginPort.LoginResultado(
                "jwt-token-agente", "ag-002", "AGENTE", "Pedro Nuevo"));

        String body = """
            {
              "token": "token-invitacion-xyz",
              "nombre": "Pedro Nuevo",
              "telefono": "3008888888",
              "correo": "pedro.nuevo@callsos.test",
              "username": "pedro.nuevo",
              "password": "Password123",
              "confirmarPassword": "Password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/agente")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rol").value("AGENTE"));
    }

    @Test
    @DisplayName("POST /registro/agente con token de invitación inválido retorna 422")
    void registroAgenteTokenInvalido() throws Exception {
        when(registrarAgentePort.ejecutar(any()))
            .thenThrow(new IllegalStateException("Token de invitación inválido o expirado"));

        String body = """
            {
              "token": "token-vencido",
              "nombre": "Pedro Nuevo",
              "telefono": "3008888888",
              "correo": "pedro.nuevo@callsos.test",
              "username": "pedro.nuevo",
              "password": "Password123",
              "confirmarPassword": "Password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/agente")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /registro/agente sin correo (Épica 8, hallazgo #6, Parte 1) retorna 400")
    void registroAgenteSinCorreo() throws Exception {
        String body = """
            {
              "token": "token-invitacion-xyz",
              "nombre": "Pedro Nuevo",
              "telefono": "3008888888",
              "username": "pedro.nuevo",
              "password": "Password123",
              "confirmarPassword": "Password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/registro/agente")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registrarAgentePort);
    }

    // ── Épica 8 (hallazgo #6, Parte 2) — recuperación de contraseña ─────────

    @Test
    @DisplayName("POST /recuperar-password con correo existente retorna 200 con mensaje genérico")
    void recuperarPasswordCorreoExistente() throws Exception {
        String body = """
            { "correo": "juan.perez@callsos.test" }
            """;

        mockMvc.perform(post("/api/v1/auth/recuperar-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    @DisplayName(
        "POST /recuperar-password con correo INEXISTENTE retorna el MISMO 200 y mensaje "
        + "genérico (anti-enumeración de cuentas) — no debe revelar que el correo no existe")
    void recuperarPasswordCorreoInexistenteRespondeIgual() throws Exception {
        String body = """
            { "correo": "no-existe@callsos.test" }
            """;

        mockMvc.perform(post("/api/v1/auth/recuperar-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(
                "Si el correo ingresado está registrado, recibirás un código "
                + "de verificación para restablecer tu contraseña."));
    }

    @Test
    @DisplayName("POST /recuperar-password sin correo retorna 400")
    void recuperarPasswordSinCorreo() throws Exception {
        mockMvc.perform(post("/api/v1/auth/recuperar-password")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(solicitarReseteoPort);
    }

    @Test
    @DisplayName("POST /recuperar-password con formato de correo inválido retorna 400")
    void recuperarPasswordCorreoInvalido() throws Exception {
        String body = """
            { "correo": "esto-no-es-un-correo" }
            """;

        mockMvc.perform(post("/api/v1/auth/recuperar-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(solicitarReseteoPort);
    }

    @Test
    @DisplayName("POST /resetear-password con token vigente retorna 200")
    void resetearPasswordExitoso() throws Exception {
        String body = """
            {
              "token": "token-reseteo-abc123",
              "nuevaPassword": "NuevaPassword123",
              "confirmarPassword": "NuevaPassword123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/resetear-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    @DisplayName("POST /resetear-password con token inválido/expirado retorna 422")
    void resetearPasswordTokenInvalido() throws Exception {
        doThrow(new IllegalStateException(
                "El token de reseteo no existe o ya no es válido."))
            .when(resetearPasswordPort).ejecutar(any(), any(), any());

        String body = """
            {
              "token": "token-vencido",
              "nuevaPassword": "NuevaPassword123",
              "confirmarPassword": "NuevaPassword123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/resetear-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /resetear-password con contraseñas que no coinciden retorna 422")
    void resetearPasswordPasswordsNoCoinciden() throws Exception {
        doThrow(new IllegalStateException("Las contraseñas no coinciden."))
            .when(resetearPasswordPort).ejecutar(any(), any(), any());

        String body = """
            {
              "token": "token-reseteo-abc123",
              "nuevaPassword": "NuevaPassword123",
              "confirmarPassword": "OtraCosa456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/resetear-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /resetear-password sin token retorna 400")
    void resetearPasswordSinToken() throws Exception {
        String body = """
            {
              "nuevaPassword": "NuevaPassword123",
              "confirmarPassword": "NuevaPassword123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/resetear-password")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(resetearPasswordPort);
    }
}