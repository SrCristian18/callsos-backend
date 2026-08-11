/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoAgente;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.port.out.AgenteByIdRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.Optional;
 
@Component
public class AgenteByidRepositoryMySQL implements AgenteByIdRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public AgenteByidRepositoryMySQL(DataSource dataSource) {
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
                Agente agente = new Agente(
                    rs.getString("id"),
                    rs.getString("nombre"),
                    rs.getString("direccion"),
                    ub,
                    rs.getString("telefono")
                );
                // FIX (Épica 4, detectado por AgenteByidRepositoryMySQLTest):
                // el SELECT ya traía "estado" pero nunca se usaba — el
                // constructor de Agente siempre deja DISPONIBLE por
                // defecto, así que este método devolvía TODO agente como
                // disponible sin importar lo que dijera la BD. Cualquier
                // caso de uso que dependiera de este puerto para saber si
                // UN agente específico estaba ocupado recibía un dato
                // falso. Mismo patrón de sincronización que ya usa
                // AgenteRepositoryMySQL.AgenteRowMapper.
                if (EstadoAgente.OCUPADO.name().equals(rs.getString("estado"))) {
                    agente.asignar();
                }
                return Optional.of(agente);
            }
            return Optional.<Agente>empty();
        }, id);
    }
}