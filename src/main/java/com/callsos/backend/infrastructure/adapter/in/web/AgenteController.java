package com.callsos.backend.infrastructure.adapter.in.web;

import com.callsos.backend.domain.port.in.RegistrarTokenFcmAgentePort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para el Agente — Épica 5.
 *
 * Mismo patrón exacto que DenuncianteController (PATCH /{id}/token):
 * hasRole("AGENTE") en SecurityConfig solo confirma que el caller ES un
 * agente, no que sea EL agente dueño del recurso {id} — el ownership
 * real se valida acá comparando authentication.getName() (actorId del
 * JWT) contra el {id} del path.
 */
@RestController
@RequestMapping("/api/v1/agentes")
public class AgenteController {

    private final RegistrarTokenFcmAgentePort registrarTokenFcm;

    public AgenteController(RegistrarTokenFcmAgentePort registrarTokenFcm) {
        this.registrarTokenFcm = registrarTokenFcm;
    }

    /**
     * PATCH /api/v1/agentes/{id}/token
     *
     * Registra o actualiza el token FCM del agente autenticado.
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
