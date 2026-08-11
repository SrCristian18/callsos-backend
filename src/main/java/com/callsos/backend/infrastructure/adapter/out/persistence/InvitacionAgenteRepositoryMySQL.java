/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

import com.callsos.backend.domain.model.InvitacionAgente;
import com.callsos.backend.domain.port.out.InvitacionAgenteRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

@Component
public class InvitacionAgenteRepositoryMySQL implements InvitacionAgenteRepositoryPort {

    private final JdbcTemplate jdbc;

    public InvitacionAgenteRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void guardar(InvitacionAgente invitacion) {
        jdbc.update(
            """
            INSERT INTO invitaciones_agente
                (token, unidad_policial_id, creado_por, fecha_creacion,
                 fecha_expiracion, usado, usado_por, fecha_uso)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            invitacion.getToken(),
            invitacion.getUnidadPolicialId(),
            invitacion.getCreadoPor(),
            Timestamp.valueOf(invitacion.getFechaCreacion()),
            Timestamp.valueOf(invitacion.getFechaExpiracion()),
            invitacion.isUsado(),
            invitacion.getUsadoPor(),
            invitacion.getFechaUso() != null
                ? Timestamp.valueOf(invitacion.getFechaUso()) : null
        );
    }

    @Override
    public Optional<InvitacionAgente> buscarPorToken(String token) {
        String sql = """
            SELECT token, unidad_policial_id, creado_por, fecha_creacion,
                   fecha_expiracion, usado, usado_por, fecha_uso
            FROM invitaciones_agente
            WHERE token = ?
            """;
        return jdbc.query(sql, new InvitacionRowMapper(), token)
            .stream().findFirst();
    }

    @Override
    public void actualizar(InvitacionAgente invitacion) {
        jdbc.update(
            """
            UPDATE invitaciones_agente
            SET usado = ?, usado_por = ?, fecha_uso = ?
            WHERE token = ?
            """,
            invitacion.isUsado(),
            invitacion.getUsadoPor(),
            invitacion.getFechaUso() != null
                ? Timestamp.valueOf(invitacion.getFechaUso()) : null,
            invitacion.getToken()
        );
    }

    private static class InvitacionRowMapper implements RowMapper<InvitacionAgente> {
        @Override
        public InvitacionAgente mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp fechaUso = rs.getTimestamp("fecha_uso");
            return InvitacionAgente.reconstituir(
                rs.getString("token"),
                rs.getString("unidad_policial_id"),
                rs.getString("creado_por"),
                rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                rs.getTimestamp("fecha_expiracion").toLocalDateTime(),
                rs.getBoolean("usado"),
                rs.getString("usado_por"),
                fechaUso != null ? fechaUso.toLocalDateTime() : null
            );
        }
    }
}
