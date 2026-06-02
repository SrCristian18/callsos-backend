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
import com.callsos.backend.domain.model.UnidadPolicial;
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
 
    /**
     * FIX: la versión anterior omitía unidad_policial_id en el INSERT/UPDATE.
     * derivarACAI() asignaba la unidad en memoria pero nunca se persistía,
     * por lo que AsignarAgenteService siempre leía un incidente con unidad null.
     *
     * Ahora guarda y actualiza unidad_policial_id correctamente.
     */
    @Override
public void guardar(Incidente incidente) {

    String unidadId = (incidente.getUnidadPolicial() != null)
        ? incidente.getUnidadPolicial().getId()
        : null;

    int filas = jdbc.update("""
        UPDATE incidentes
        SET descripcion = ?,
            estado = ?,
            latitud = ?,
            longitud = ?,
            unidad_policial_id = ?
        WHERE id = ?
        """,
        incidente.getDescripcion(),
        incidente.getEstado().name(),
        incidente.getUbicacion().getLatitud(),
        incidente.getUbicacion().getLongitud(),
        unidadId,
        incidente.getId()
    );

    if (filas == 0) {
        jdbc.update("""
            INSERT INTO incidentes
            (id, fecha_hora, tipo, descripcion, estado,
             latitud, longitud, denunciante_id, unidad_policial_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            incidente.getId(),
            incidente.getFechaHora(),
            incidente.getTipo().name(),
            incidente.getDescripcion(),
            incidente.getEstado().name(),
            incidente.getUbicacion().getLatitud(),
            incidente.getUbicacion().getLongitud(),
            incidente.getDenunciante().getId(),
            unidadId
        );
    }
}
 
    /**
     * FIX: la versión anterior no leía unidad_policial_id de la BD.
     * AsignarAgenteService cargaba el incidente y encontraba unidad null
     * aunque ya estuviera asignada, fallando siempre con excepción.
     *
     * Ahora hace LEFT JOIN con unidades_policiales y reconstituye
     * la UnidadPolicial si existe.
     */
    @Override
    public Optional<Incidente> buscarPorId(String id) {
        String sql = """
            SELECT i.id, i.fecha_hora, i.tipo, i.descripcion, i.estado,
                   i.latitud, i.longitud, i.denunciante_id,
                   d.nombre    AS d_nombre,
                   d.origen    AS d_origen,
                   d.telefono  AS d_telefono,
                   d.correo    AS d_correo,
                   d.token_fcm AS d_token_fcm,
                   i.unidad_policial_id,
                   u.nombre    AS u_nombre,
                   u.direccion AS u_direccion,
                   u.latitud   AS u_latitud,
                   u.longitud  AS u_longitud,
                   u.telefono  AS u_telefono
            FROM incidentes i
            JOIN denunciantes d ON d.id = i.denunciante_id
            LEFT JOIN unidades_policiales u ON u.id = i.unidad_policial_id
            WHERE i.id = ?
            """;
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();
 
            Denunciante denunciante = new Denunciante(
                rs.getString("denunciante_id"),
                rs.getString("d_nombre"),
                rs.getString("d_origen"),
                rs.getString("d_telefono"),
                rs.getString("d_correo"),
                rs.getString("d_token_fcm")
            );
 
            Incidente incidente = new Incidente(
                rs.getString("id"),
                TipoIncidente.valueOf(rs.getString("tipo")),
                rs.getString("descripcion"),
                new Ubicacion(rs.getDouble("latitud"), rs.getDouble("longitud")),
                denunciante
            );
 
            // Reconstituir estado real desde BD (el constructor lo pone en CREADO)
            incidente.reconstituirEstado(
                EstadoIncidente.valueOf(rs.getString("estado")));
 
            // Reconstituir la unidad policial si existe
            String unidadId = rs.getString("unidad_policial_id");
            if (unidadId != null) {
                UnidadPolicial unidad = new UnidadPolicial(
                    unidadId,
                    rs.getString("u_nombre"),
                    rs.getString("u_direccion"),
                    new Ubicacion(rs.getDouble("u_latitud"), rs.getDouble("u_longitud")),
                    rs.getString("u_telefono")
                );
                incidente.reconstituirUnidad(unidad);
            }
 
            return Optional.of(incidente);
        }, id);
    }
 
    @Override
    public void actualizarEstado(String id, EstadoIncidente estado) {
        jdbc.update(
            "UPDATE incidentes SET estado = ? WHERE id = ?",
            estado.name(), id
        );
    }
}