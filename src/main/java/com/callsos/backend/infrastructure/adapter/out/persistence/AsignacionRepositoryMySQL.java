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
 * La búsqueda por incidente recupera la asignación más reciente ACTIVA,
 * ya que un incidente puede tener varias asignaciones históricas pero
 * solo una activa en un momento dado.
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
    
    @Override
    public Optional<Asignacion> buscarPorIncidente(String incidenteId) {
        String sql = """
            SELECT id, estado
            FROM asignaciones
            WHERE incidente_id = ?
              AND estado = 'ACTIVA'
            ORDER BY fecha_asignacion DESC
            LIMIT 1
            """;
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                return Optional.<Asignacion>empty();
            }
            return Optional.empty();
        }, incidenteId);
    }
}
