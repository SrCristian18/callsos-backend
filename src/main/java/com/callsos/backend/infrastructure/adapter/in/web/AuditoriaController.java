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
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
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
 *
 * FIX (post-Épica 2, detectado en revisión antes de Épica 3): el chequeo
 * de AGENTE originalmente usaba incidente.getAsignaciones() — una lista
 * que SOLO se llena en memoria vía agregarAsignacion() durante el mismo
 * request en que se crea la asignación. IncidenteRepositoryMySQL.buscarPorId()
 * NUNCA reconstituye esa lista desde la tabla `asignaciones` al leer de
 * BD (no hace ese JOIN) — así que en un request real y separado (como
 * este, GET /auditoria/incidente/{id}), la lista siempre llegaba vacía y
 * el AGENTE dueño de la asignación recibía 403 igual que uno ajeno. El
 * test unitario no lo detectó porque construye el Incidente en memoria y
 * llama agregarAsignacion() antes de mockear el repositorio, ocultando el
 * problema. Se corrige consultando AsignacionRepositoryPort.buscarPorIncidente()
 * — la misma fuente de verdad (consulta real a la tabla `asignaciones`)
 * que ya usa MarcarAgenteEnCaminoService.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {
    
    private final AuditoriaRepositoryPort auditoriaRepository;
    private final IncidenteRepositoryPort incidenteRepository;
    private final AsignacionRepositoryPort asignacionRepository;
 
    public AuditoriaController(AuditoriaRepositoryPort auditoriaRepository,
                               IncidenteRepositoryPort incidenteRepository,
                               AsignacionRepositoryPort asignacionRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.incidenteRepository = incidenteRepository;
        this.asignacionRepository = asignacionRepository;
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
                case "AGENTE" -> asignacionRepository.buscarPorIncidente(id)
                    .map(a -> a.getAgente().getId())
                    .filter(agenteId -> agenteId.equals(actorId))
                    .isPresent();
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