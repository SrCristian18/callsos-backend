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
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
 
/**
 * Adaptador de salida: implementa AgenteRepositoryPort con JDBC + MySQL.
 */
@Component
public class AgenteRepositoryMySQL implements AgenteRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public AgenteRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public List<Agente> obtenerDisponibles() {
        String sql = """
            SELECT id, nombre, direccion, telefono, latitud, longitud, estado
            FROM agentes
            WHERE estado = 'DISPONIBLE'
            """;
        return jdbc.query(sql, new AgenteRowMapper());
    }
 
    /**
     * FIX del bug contains(): el filtro es SQL → compara IDs en BD,
     * no referencias de objetos Java en memoria.
     */
    @Override
    public List<Agente> obtenerDisponiblesPorUnidad(String unidadPolicialId) {
        String sql = """
            SELECT id, nombre, direccion, telefono, latitud, longitud, estado
            FROM agentes
            WHERE estado = 'DISPONIBLE'
              AND unidad_policial_id = ?
            ORDER BY nombre ASC
            """;
        return jdbc.query(sql, new AgenteRowMapper(), unidadPolicialId);
    }
 
    @Override
    public void actualizarEstado(Agente agente) {
        jdbc.update(
            "UPDATE agentes SET estado = ? WHERE id = ?",
            agente.getEstado().name(),
            agente.getId()
        );
    }
 
    private static class AgenteRowMapper implements RowMapper<Agente> {
        @Override
        public Agente mapRow(ResultSet rs, int rowNum) throws SQLException {
            double lat = rs.getDouble("latitud");
            double lon = rs.getDouble("longitud");
            // Agente puede no tener ubicación registrada aún
            Ubicacion ubicacion = (lat == 0 && lon == 0)
                ? null
                : new Ubicacion(lat, lon);
            Agente agente = new Agente(
                rs.getString("id"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                ubicacion,
                rs.getString("telefono")
            );
            // Si en BD está OCUPADO, sincronizar estado (sin efectos de dominio extra)
            if (EstadoAgente.OCUPADO.name().equals(rs.getString("estado"))) {
                agente.asignar();
            }
            return agente;
        }
    }
}
