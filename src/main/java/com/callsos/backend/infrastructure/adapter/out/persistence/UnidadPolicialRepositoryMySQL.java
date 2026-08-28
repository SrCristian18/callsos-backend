/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
 
/**
 * Adaptador de salida: implementa UnidadPolicialRepositoryPort con JDBC + MySQL.
 * (Equivalente al CAIRepositoryMySQL del diagrama v3 — renombrado
 *  para mantener la semántica acordada: CAI → UnidadPolicial.)
 *
 * La búsqueda por proximidad usa la fórmula de Haversine aproximada
 * via SQL para encontrar la unidad más cercana a la ubicación del incidente.
 */
@Component
public class UnidadPolicialRepositoryMySQL implements UnidadPolicialRepositoryPort{
 
     private final JdbcTemplate jdbc;
 
    public UnidadPolicialRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public Optional<UnidadPolicial> buscarPorUbicacion(Ubicacion ubicacion) {
        String sql = """
            SELECT id, nombre, direccion, latitud, longitud, telefono, token_fcm,
                   (6371 * ACOS(
                       COS(RADIANS(?)) * COS(RADIANS(latitud)) *
                       COS(RADIANS(longitud) - RADIANS(?)) +
                       SIN(RADIANS(?)) * SIN(RADIANS(latitud))
                   )) AS distancia_km
            FROM unidades_policiales
            ORDER BY distancia_km ASC
            LIMIT 1
            """;
        return jdbc.query(
            sql,
            new UnidadPolicialRowMapper(),
            ubicacion.getLatitud(),
            ubicacion.getLongitud(),
            ubicacion.getLatitud()
        ).stream().findFirst();
    }
 
    @Override
    public Optional<UnidadPolicial> buscarPorId(String id) {
        String sql = """
            SELECT id, nombre, direccion, latitud, longitud, telefono, token_fcm
            FROM unidades_policiales
            WHERE id = ?
            """;
        return jdbc.query(sql, new UnidadPolicialRowMapper(), id)
            .stream().findFirst();
    }

    @Override
    public void actualizarTokenFcm(String unidadPolicialId, String tokenFcm) {
        jdbc.update(
            "UPDATE unidades_policiales SET token_fcm = ? WHERE id = ?",
            tokenFcm, unidadPolicialId
        );
    }
 
    private static class UnidadPolicialRowMapper implements RowMapper<UnidadPolicial> {
        @Override
        public UnidadPolicial mapRow(ResultSet rs, int rowNum) throws SQLException {
            UnidadPolicial unidad = new UnidadPolicial(
                rs.getString("id"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                new Ubicacion(rs.getDouble("latitud"), rs.getDouble("longitud")),
                rs.getString("telefono")
            );
            unidad.setTokenFcm(rs.getString("token_fcm"));
            return unidad;
        }
    }
}