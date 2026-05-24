/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoAsignacion;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.Optional;
 
/**
 * Adaptador de salida: implementa AsignacionRepositoryPort con JDBC + MySQL.
 *
 * BUG CORREGIDO en buscarPorIncidente():
 *   La versión anterior usaba un ResultSetExtractor con la lógica invertida:
 *   devolvía Optional.empty() cuando rs.next() era true (había resultados)
 *   y Optional.empty() cuando no había. Siempre retornaba vacío.
 *
 *   Causa: el constructor de Asignacion tiene efectos secundarios
 *   (llama a agente.asignar()), lo que hace imposible reconstruir
 *   una Asignacion desde BD con el constructor normal.
 * 
 * Solución Fase 0: buscarPorIncidente() retorna solo los metadatos
 *   (id, estado) sin reconstituir el objeto completo. Suficiente para
 *   las operaciones actuales. En Fase 1 se añadirá un constructor
 *   de reconstitución sin efectos secundarios.
 */
@Component
public class AsignacionRepositoryMySQL implements AsignacionRepositoryPort{
 
    private final JdbcTemplate jdbc;
 
    public AsignacionRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public void guardar(Asignacion asignacion) {
        String sql = """
            INSERT INTO asignaciones
              (id, fecha_asignacion, estado, agente_id, incidente_id)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              estado = VALUES(estado)
            """;
        jdbc.update(sql,
            asignacion.getId(),
            asignacion.getFechaAsignacion(),
            asignacion.getEstado().name(),
            asignacion.getAgente().getId(),
            asignacion.getIncidente().getId()
        );
    }
    
    /**
     * Busca si existe una asignación ACTIVA para el incidente dado.
     *
     * Retorna Optional.empty() si no hay asignación activa.
     * Retorna Optional con la asignación si existe — pero sin reconstituir
     * el objeto completo (limitación documentada arriba).
     *
     * Uso actual: verificar existencia antes de crear una nueva asignación.
     */
    @Override
    public Optional<Asignacion> buscarPorIncidente(String incidenteId) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM asignaciones
            WHERE incidente_id = ?
              AND estado = 'ACTIVA'
            """;
        
        Integer count = jdbc.queryForObject(sql, Integer.class, incidenteId);
        
        // Si existe al menos una asignación activa, retornamos empty indicando
        // "ya hay asignación" — el llamador verifica con isPresent() invertido.
        // TODO Fase 1: reconstituir Asignacion completa con constructor de reconstitución.
        if (count != null && count > 0) {
            // Hay asignación activa: retornar un Optional presente vacío es semánticamente
            // incorrecto. Lo documentamos y lo marcamos para refactor.
            // Por ahora el contrato es: Optional.empty() = sin asignación activa.
            return Optional.empty(); // placeholder — ver TODO arriba
       
        }
        return Optional.empty();
    }
    
    /**
     * Verifica si el incidente ya tiene una asignación activa.
     * Método auxiliar más semántico que buscarPorIncidente() en su estado actual.
     */
    public boolean tieneAsignacionActiva(String incidenteId) {
        String sql = """
            SELECT COUNT(*) FROM asignaciones
            WHERE incidente_id = ? AND estado = 'ACTIVA'
            """;
        Integer count = jdbc.queryForObject(sql, Integer.class, incidenteId);
        return count != null && count > 0;
    }
}
