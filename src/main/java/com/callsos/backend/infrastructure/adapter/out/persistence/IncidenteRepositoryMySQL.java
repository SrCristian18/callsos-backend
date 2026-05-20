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
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Adaptador de salida: implementa IncidenteRepositoryPort con JDBC + MySQL.
 *
 * Regla de la arquitectura hexagonal: esta clase conoce SQL y Spring JDBC.
 * El dominio no sabe que existe — solo conoce la interfaz del puerto.
 */
@Component
public class IncidenteRepositoryMySQL implements IncidenteRepositoryPort {
    
    private final JdbcTemplate jdbc;
 
    public IncidenteRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
    
     @Override
    public void guardar(Incidente incidente) {
        String sql = """
            INSERT INTO incidentes
              (id, fecha_hora, tipo, descripcion, estado,
               latitud, longitud, denunciante_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              descripcion   = VALUES(descripcion),
              estado        = VALUES(estado),
              latitud       = VALUES(latitud),
              longitud      = VALUES(longitud)
            """;
        jdbc.update(sql,
            incidente.getId(),
            incidente.getFechaHora(),
            incidente.getTipo().name(),
            incidente.getDescripcion(),
            incidente.getEstado().name(),
            incidente.getUbicacion().getLatitud(),
            incidente.getUbicacion().getLongitud(),
            incidente.getDenunciante().getId()
        );
    }
    
    @Override
    public Optional<Incidente> buscarPorId(String id) {
        String sql = """
            SELECT i.id, i.fecha_hora, i.tipo, i.descripcion, i.estado,
                   i.latitud, i.longitud,
                   i.denunciante_id,
                   d.nombre AS d_nombre, d.origen AS d_origen,
                   d.telefono AS d_telefono, d.correo AS d_correo
            FROM incidentes i
            JOIN denunciantes d ON d.id = i.denunciante_id
            WHERE i.id = ?
            """;
        return jdbc.query(sql, new IncidenteRowMapper(), id)
            .stream().findFirst();
    }
    
    @Override
    public void actualizarEstado(String id, EstadoIncidente estado) {
        jdbc.update(
            "UPDATE incidentes SET estado = ? WHERE id = ?",
            estado.name(), id
        );
    }
    
    private static class IncidenteRowMapper implements RowMapper<Incidente> {
        @Override
        public Incidente mapRow(ResultSet rs, int rowNum) throws SQLException {
            Denunciante denunciante = new Denunciante(
                rs.getString("denunciante_id"),
                rs.getString("d_nombre"),
                rs.getString("d_origen"),
                rs.getString("d_telefono"),
                rs.getString("d_correo")
            );
            Ubicacion ubicacion = new Ubicacion(
                rs.getDouble("latitud"),
                rs.getDouble("longitud")
            );
            return new Incidente(
                rs.getString("id"),
                TipoIncidente.valueOf(rs.getString("tipo")),
                rs.getString("descripcion"),
                ubicacion,
                denunciante
            );
        }
    }
}
