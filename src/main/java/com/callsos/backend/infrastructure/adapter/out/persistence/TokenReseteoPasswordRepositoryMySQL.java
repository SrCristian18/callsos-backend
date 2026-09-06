/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

import com.callsos.backend.domain.model.TokenReseteoPassword;
import com.callsos.backend.domain.port.out.TokenReseteoPasswordRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Adaptador de salida MySQL para TokenReseteoPassword.
 * Épica 8 (hallazgo #6, Parte 2). Mismo patrón que InvitacionAgenteRepositoryMySQL.
 */
@Component
public class TokenReseteoPasswordRepositoryMySQL implements TokenReseteoPasswordRepositoryPort {

    private final JdbcTemplate jdbc;

    public TokenReseteoPasswordRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void guardar(TokenReseteoPassword token) {
        jdbc.update(
            """
            INSERT INTO tokens_reseteo_password
                (token, actor_id, fecha_creacion, fecha_expiracion, usado, fecha_uso)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            token.getToken(),
            token.getActorId(),
            Timestamp.valueOf(token.getFechaCreacion()),
            Timestamp.valueOf(token.getFechaExpiracion()),
            token.isUsado(),
            token.getFechaUso() != null ? Timestamp.valueOf(token.getFechaUso()) : null
        );
    }

    @Override
    public Optional<TokenReseteoPassword> buscarPorToken(String token) {
        String sql = """
            SELECT token, actor_id, fecha_creacion, fecha_expiracion, usado, fecha_uso
            FROM tokens_reseteo_password
            WHERE token = ?
            """;
        return jdbc.query(sql, new TokenRowMapper(), token)
            .stream().findFirst();
    }

    @Override
    public void actualizar(TokenReseteoPassword token) {
        jdbc.update(
            """
            UPDATE tokens_reseteo_password
            SET usado = ?, fecha_uso = ?
            WHERE token = ?
            """,
            token.isUsado(),
            token.getFechaUso() != null ? Timestamp.valueOf(token.getFechaUso()) : null,
            token.getToken()
        );
    }

    private static class TokenRowMapper implements RowMapper<TokenReseteoPassword> {
        @Override
        public TokenReseteoPassword mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp fechaUso = rs.getTimestamp("fecha_uso");
            return TokenReseteoPassword.reconstituir(
                rs.getString("token"),
                rs.getString("actor_id"),
                rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                rs.getTimestamp("fecha_expiracion").toLocalDateTime(),
                rs.getBoolean("usado"),
                fechaUso != null ? fechaUso.toLocalDateTime() : null
            );
        }
    }
}
