package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.in.ConsultarAgentesDisponiblesPorCaiPort;
import com.callsos.backend.domain.port.in.RegistrarTokenFcmUnidadPort;
import com.callsos.backend.infrastructure.adapter.in.web.dto.AgenteDisponibleResponse;
import com.callsos.backend.infrastructure.adapter.in.web.mapper.AgenteMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST — recursos de CAI (unidad policial).
 *
 * FIX: resuelve el Gap 3 de deuda_backend.md — hasta ahora no existía
 * ningún endpoint para que el operador de un CAI viera qué agentes
 * disponibles tiene antes de asignar un incidente.
 *
 * Endpoints:
 *   GET   /{caiId}/agentes/disponibles → OPERADOR_CAI / COMANDO
 *   PATCH /{id}/token                  → OPERADOR_CAI (Épica 5)
 */
@RestController
@RequestMapping("/api/v1/cais")
public class CaiController {

    private final ConsultarAgentesDisponiblesPorCaiPort consultarDisponibles;
    private final RegistrarTokenFcmUnidadPort registrarTokenFcm;

    public CaiController(ConsultarAgentesDisponiblesPorCaiPort consultarDisponibles,
                         RegistrarTokenFcmUnidadPort registrarTokenFcm) {
        this.consultarDisponibles = consultarDisponibles;
        this.registrarTokenFcm    = registrarTokenFcm;
    }

    /**
     * GET /{caiId}/agentes/disponibles — agentes en estado DISPONIBLE
     * dentro del CAI indicado.
     *
     * FIX (Épica 8, hallazgo #3 — Regla 5, aislamiento entre CAIs): antes
     * no se validaba que caiId coincidiera con el actorId del operador
     * autenticado — cualquier OPERADOR_CAI podía pasar el ID de OTRO CAI
     * y ver sus agentes disponibles. Mismo patrón que AuditoriaController:
     *   - OPERADOR_CAI: solo puede consultar SU PROPIO CAI (actorId ==
     *     unidadPolicialId por convención, ver P9 en el análisis técnico).
     *   - COMANDO: visión global, sin restricción adicional.
     */
    @GetMapping("/{caiId}/agentes/disponibles")
    public ResponseEntity<List<AgenteDisponibleResponse>> agentesDisponibles(
            @PathVariable String caiId, Authentication authentication) {

        String actorId = authentication.getName();
        String rol = authentication.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("");

        if ("OPERADOR_CAI".equals(rol) && !caiId.equals(actorId)) {
            throw new AccesoDenegadoException(
                "El operador autenticado no pertenece a este CAI.");
        }

        List<Agente> agentes = consultarDisponibles.ejecutar(caiId);
        return ResponseEntity.ok(AgenteMapper.toResponseList(agentes));
    }

    /**
     * PATCH /api/v1/cais/{id}/token — Épica 5.
     *
     * Registra o actualiza el token FCM del CAI autenticado. A diferencia
     * de agentesDisponibles() arriba, este endpoint SÍ valida ownership
     * (mismo patrón que DenuncianteController/AgenteController): un
     * OPERADOR_CAI solo puede registrar el token de SU PROPIA unidad
     * (actorId == unidadPolicialId por convención, ver P9 en el análisis
     * técnico) — sin esta validación, cualquier operador autenticado
     * podría sobrescribir el token de otro CAI y cortarle las
     * notificaciones.
     */
    @PatchMapping("/{id}/token")
    public ResponseEntity<Void> registrarToken(
            @PathVariable String id,
            @Valid @RequestBody TokenFcmRequest request,
            Authentication authentication) {

        String actorIdDelJwt = authentication.getName();
        if (!actorIdDelJwt.equals(id)) {
            return ResponseEntity.status(403).build();
        }

        registrarTokenFcm.ejecutar(id, request.tokenFcm());
        return ResponseEntity.noContent().build();
    }

    public record TokenFcmRequest(
        @NotBlank(message = "El tokenFcm es obligatorio")
        String tokenFcm
    ) {}
}