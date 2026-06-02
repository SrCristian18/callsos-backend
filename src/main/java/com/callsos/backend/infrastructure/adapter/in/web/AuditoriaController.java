/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
 
/**
 * Adaptador de entrada: consulta del historial de auditoría.
 *
 * GET /api/v1/auditoria/incidente/{id}
 * Solo accesible para OPERADOR_CAI y COMANDO (SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {
    
    private final AuditoriaRepositoryPort auditoriaRepository;
 
    public AuditoriaController(AuditoriaRepositoryPort auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }
 
    @GetMapping("/incidente/{id}")
    public ResponseEntity<List<AuditoriaIncidente>> historial(@PathVariable String id) {
        return ResponseEntity.ok(auditoriaRepository.buscarPorIncidente(id));
    }
}
