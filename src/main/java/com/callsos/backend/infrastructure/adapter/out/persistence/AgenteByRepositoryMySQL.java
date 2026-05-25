/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.out.AgenteByIdRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.Optional;
 
@Component
public class AgenteByRepositoryMySQL implements AgenteByIdRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public AgenteByIdRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public Optional<Agente> buscarPorId(String id) {
        String sql = """
            SELECT id, nombre, direccion, telefono, latitud, longitud, estado
            FROM agentes WHERE id = ?
            """;
        return jdbc.query(sql, rs -> {
            if (rs.next()) {
                double lat = rs.getDouble("latitud");
                double lon = rs.getDouble("longitud");
                Ubicacion ub = (lat == 0 && lon == 0) ? null : new Ubicacion(lat, lon);
                return Optional.of(new Agente(
                    rs.getString("id"),
                    rs.getString("nombre"),
                    rs.getString("direccion"),
                    ub,
                    rs.getString("telefono")
                ));
            }
            return Optional.<Agente>empty();
        }, id);
    }
}
