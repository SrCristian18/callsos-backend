/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
 
/**
 * Adaptador de salida: implementa DenuncianteRepositoryPort con JDBC + MySQL.
 * (Equivalente al UsuarioRepositoryMySQL del diagrama v3 — renombrado
 *  para mantener la semántica acordada: Usuario → Denunciante.)
 */
@Component
public class DenuncianteRepositoryMySQL implements DenuncianteRepositoryPort {
    
    private final JdbcTemplate jdbc;
 
    public DenuncianteRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public Optional<Denunciante> buscarPorId(String id) {
        String sql = """
            SELECT id, nombre, origen, telefono, correo
            FROM denunciantes
            WHERE id = ?
            """;
        return jdbc.query(sql, new DenuncianteRowMapper(), id)
            .stream().findFirst();
    }
    
     private static class DenuncianteRowMapper implements RowMapper<Denunciante> {
        @Override
        public Denunciante mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Denunciante(
                rs.getString("id"),
                rs.getString("nombre"),
                rs.getString("origen"),
                rs.getString("telefono"),
                rs.getString("correo")
            );
        }
    }
}
