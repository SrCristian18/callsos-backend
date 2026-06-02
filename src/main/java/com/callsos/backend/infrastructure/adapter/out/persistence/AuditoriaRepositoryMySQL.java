/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.AuditoriaIncidente;
import com.callsos.backend.domain.port.out.AuditoriaRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.List;
 
@Component
public class AuditoriaRepositoryMySQL implements AuditoriaRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public AuditoriaRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public void registrar(AuditoriaIncidente auditoria) {
        jdbc.update(
            """
            INSERT INTO auditoria_incidente
              (incidente_id, estado_anterior, estado_nuevo,
               actor_id, actor_rol, timestamp, detalle)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            auditoria.getIncidenteId(),
            auditoria.getEstadoAnterior() != null
                ? auditoria.getEstadoAnterior().name() : null,
            auditoria.getEstadoNuevo().name(),
            auditoria.getActorId(),
            auditoria.getActorRol(),
            auditoria.getTimestamp(),
            auditoria.getDetalle()
        );
    }
 
    @Override
    public List<AuditoriaIncidente> buscarPorIncidente(String incidenteId) {
        return jdbc.query(
            """
            SELECT incidente_id, estado_anterior, estado_nuevo,
                   actor_id, actor_rol, timestamp, detalle
            FROM auditoria_incidente
            WHERE incidente_id = ?
            ORDER BY timestamp ASC
            """,
            (rs, i) -> new AuditoriaIncidente(
                rs.getString("incidente_id"),
                rs.getString("estado_anterior") != null
                    ? EstadoIncidente.valueOf(rs.getString("estado_anterior")) : null,
                EstadoIncidente.valueOf(rs.getString("estado_nuevo")),
                rs.getString("actor_id"),
                rs.getString("actor_rol"),
                rs.getString("detalle")
            ),
            incidenteId
        );
    }
}
