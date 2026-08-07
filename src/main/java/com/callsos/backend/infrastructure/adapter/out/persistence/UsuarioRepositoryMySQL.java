/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.Optional;
 
/**
 * Adaptador de salida: busca credenciales de usuario en la tabla usuarios.
 *
 * Solo busca usuarios con activo = TRUE — los desactivados no pueden
 * iniciar sesión aunque existan en BD.
 */
@Component
public class UsuarioRepositoryMySQL implements UsuarioRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public UsuarioRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public Optional<UsuarioCredencial> buscarPorUsername(String username) {
        return jdbc.query(
            """
            SELECT id, username, password, rol, actor_id
            FROM usuarios
            WHERE username = ? AND activo = TRUE
            """,
            (rs, i) -> new UsuarioCredencial(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("rol"),
                rs.getString("actor_id")
            ),
            username
        ).stream().findFirst();
    }

    @Override
    public boolean existePorUsername(String username) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM usuarios WHERE username = ?",
            Integer.class, username
        );
        return count != null && count > 0;
    }

    @Override
    public void guardar(String id, String username, String passwordHash,
                         String rol, String actorId) {
        jdbc.update(
            """
            INSERT INTO usuarios (id, username, password, rol, actor_id, activo)
            VALUES (?, ?, ?, ?, ?, TRUE)
            """,
            id, username, passwordHash, rol, actorId
        );
    }
}