package com.callsos.backend.infrastructure.adapter.in.web;
 
import com.callsos.backend.domain.port.in.RegistrarTokenFcmPort;
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
 * Adaptador de entrada REST para el Denunciante.
 *
 * SEGURIDAD — validación de ownership:
 *   El endpoint PATCH /{id}/token está protegido con hasRole("DENUNCIANTE"),
 *   pero eso solo garantiza que el caller ES un denunciante, no que sea
 *   EL denunciante dueño del recurso.
 *
 *   Sin ownership validation, un denunciante A podría pasar el ID de B
 *   y sobrescribir su tokenFcm — B dejaría de recibir notificaciones.
 *
 *   La validación extrae el actorId del JWT (el ID real del denunciante
 *   autenticado) y lo compara contra el {id} del path. Si no coinciden,
 *   lanza 403 Forbidden antes de llamar al caso de uso.
 */
@RestController
@RequestMapping("/api/v1/denunciantes")
public class DenuncianteController {
 
    private final RegistrarTokenFcmPort registrarTokenFcm;
 
    public DenuncianteController(RegistrarTokenFcmPort registrarTokenFcm) {
        this.registrarTokenFcm = registrarTokenFcm;
    }
 
    /**
     * PATCH /api/v1/denunciantes/{id}/token
     *
     * Registra o actualiza el token FCM del denunciante.
     *
     * @param id             ID del denunciante en el path
     * @param request        Body con el tokenFcm
     * @param authentication Inyectado por Spring Security — contiene el actorId del JWT
     */
    @PatchMapping("/{id}/token")
    public ResponseEntity<Void> registrarToken(
            @PathVariable String id,
            @Valid @RequestBody TokenFcmRequest request,
            Authentication authentication) {
 
        // OWNERSHIP: el actorId del JWT debe coincidir con el {id} del path.
        // authentication.getName() retorna el subject del JWT = actorId.
        String actorIdDelJwt = authentication.getName();
        if (!actorIdDelJwt.equals(id)) {
            // 403 Forbidden: autenticado pero sin autorización sobre este recurso
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