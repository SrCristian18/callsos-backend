/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
 
/**
 * Adaptador de entrada: consulta del historial de auditoría.
 *
 * GET /api/v1/auditoria/incidente/{id}
 *
 * Épica 2 (fix P7 — Regla 5): antes cualquier OPERADOR_CAI o COMANDO podía
 * consultar la auditoría de CUALQUIER incidente, sin importar de qué CAI
 * fuera — "CAI A no puede ver auditoría de CAI B" estaba violado. Ahora
 * se filtra por actor:
 *   - DENUNCIANTE: solo si es el dueño del incidente.
 *   - AGENTE:      solo si tiene una asignación sobre ese incidente.
 *   - OPERADOR_CAI: solo si el incidente pertenece a su unidad (mismo
 *     supuesto actorId == unidadPolicialId ya usado en
 *     IncidenteController.porCAI() — ver P9 en el análisis técnico).
 *   - COMANDO: acceso global, sin restricción adicional.
 * SecurityConfig ahora permite los 4 roles en esta ruta — el filtrado
 * real de "es TU incidente" vive acá, no en la config de Spring Security
 * (mismo patrón que ActualizarTipoIncidenteService).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {
    
    private final AuditoriaRepositoryPort auditoriaRepository;
    private final IncidenteRepositoryPort incidenteRepository;
 
    public AuditoriaController(AuditoriaRepositoryPort auditoriaRepository,
                               IncidenteRepositoryPort incidenteRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.incidenteRepository = incidenteRepository;
    }
 
    @GetMapping("/incidente/{id}")
    public ResponseEntity<List<AuditoriaIncidente>> historial(
            @PathVariable String id, Authentication authentication) {

        String actorId = authentication.getName();
        String rol = authentication.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("");

        if (!"COMANDO".equals(rol)) {
            Incidente incidente = incidenteRepository
                .buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Incidente no encontrado: " + id));

            boolean autorizado = switch (rol) {
                case "DENUNCIANTE" -> incidente.getDenunciante().getId().equals(actorId);
                case "AGENTE" -> incidente.getAsignaciones().stream()
                    .anyMatch(a -> a.getAgente().getId().equals(actorId));
                case "OPERADOR_CAI" -> incidente.getUnidadPolicial() != null
                    && incidente.getUnidadPolicial().getId().equals(actorId);
                default -> false;
            };

            if (!autorizado) {
                throw new AccesoDenegadoException(
                    "No tiene autorización para consultar la auditoría de este incidente.");
            }
        }

        return ResponseEntity.ok(auditoriaRepository.buscarPorIncidente(id));
    }
}
