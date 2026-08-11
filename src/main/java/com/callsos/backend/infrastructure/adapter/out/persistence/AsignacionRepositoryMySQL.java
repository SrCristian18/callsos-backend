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
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Optional;
 
@Component
public class AsignacionRepositoryMySQL implements AsignacionRepositoryPort{
 
    private final JdbcTemplate jdbc;
 
    public AsignacionRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public void guardar(Asignacion asignacion) {
        // FIX (Épica 4): "ON DUPLICATE KEY UPDATE" es sintaxis exclusiva de
        // MySQL — H2 (usado en @JdbcTest) no la puede parsear, fallaba con
        // BadSqlGrammarException incluso en el primer INSERT. Se reemplaza
        // por UPDATE-si-existe / INSERT-si-no (mismo patrón que
        // IncidenteRepositoryMySQL.guardar()), SQL portátil. No afecta a
        // intentarReservar(), que es el método realmente sensible a
        // condiciones de carrera y ya usa un UPDATE condicional atómico
        // aparte — esto es solo el guardado administrativo de la Asignacion.
        int filas = jdbc.update(
            "UPDATE asignaciones SET estado = ? WHERE id = ?",
            asignacion.getEstado().name(),
            asignacion.getId()
        );

        if (filas == 0) {
            jdbc.update(
                """
                INSERT INTO asignaciones
                  (id, fecha_asignacion, estado, agente_id, incidente_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                asignacion.getId(),
                asignacion.getFechaAsignacion(),
                asignacion.getEstado().name(),
                asignacion.getAgente().getId(),
                asignacion.getIncidente().getId()
            );
        }
    }
    
    /**
     * FIX: implementación real de buscarPorIncidente().
     *
     * ANTES: siempre retornaba Optional.empty() — placeholder documentado.
     *
     * AHORA: hace JOIN con agentes para reconstituir el Agente,
     * y usa Asignacion.reconstituir() (factory method sin efectos de dominio)
     * para construir el objeto sin disparar agente.asignar() nuevamente.
     *
     * Nota: Denuncia se pasa como null porque la reconstitución completa
     * requeriría JOIN adicional con denuncias e incidentes. Para el uso
     * actual (verificar existencia y estado) es suficiente.
     * En Fase 2 se puede extender el JOIN si se necesita la Denuncia completa.
     */
    @Override
    public Optional<Asignacion> buscarPorIncidente(String incidenteId) {
        String sql = """
            SELECT a.id, a.fecha_asignacion, a.estado,
                   ag.id AS ag_id, ag.nombre AS ag_nombre,
                   ag.direccion AS ag_dir, ag.telefono AS ag_tel,
                   ag.latitud AS ag_lat, ag.longitud AS ag_lon
            FROM asignaciones a
            JOIN agentes ag ON ag.id = a.agente_id
            WHERE a.incidente_id = ?
              AND a.estado = 'ACTIVA'
            ORDER BY a.fecha_asignacion DESC
            LIMIT 1
            """;
        
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();
 
            double lat = rs.getDouble("ag_lat");
            double lon = rs.getDouble("ag_lon");
            Ubicacion ubicacion = (lat == 0 && lon == 0)
                ? null : new Ubicacion(lat, lon);
 
            Agente agente = new Agente(
                rs.getString("ag_id"),
                rs.getString("ag_nombre"),
                rs.getString("ag_dir"),
                ubicacion,
                rs.getString("ag_tel")
            );
 
            return Optional.of(Asignacion.reconstituir(
                rs.getString("id"),
                rs.getObject("fecha_asignacion", LocalDateTime.class),
                EstadoAsignacion.valueOf(rs.getString("estado")),
                agente,
                null   // Denuncia: JOIN extendido pendiente Fase 2
            ));
        }, incidenteId);
    }
    
    /** Método auxiliar semántico — usado internamente cuando solo se necesita saber si existe. */
    public boolean tieneAsignacionActiva(String incidenteId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asignaciones WHERE incidente_id = ? AND estado = 'ACTIVA'",
            Integer.class, incidenteId);
        return count != null && count > 0;
    }
}