/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
 
/**
 * Adaptador de salida: persistencia de posiciones GPS con JDBC + MySQL.
 *
 * La tabla ubicaciones_agente ya fue creada en schema.sql (Fase 0):
 *   id BIGINT AUTO_INCREMENT, agente_id, incidente_id,
 *   latitud DECIMAL(10,8), longitud DECIMAL(11,8), timestamp DATETIME
 *
 * Índices existentes:
 *   idx_agente_ts    (agente_id, timestamp) — para ultimaPosicion()
 *   idx_incidente_ts (incidente_id, timestamp) — para buscarPorIncidente()
 */
@Component
public class UbicacionAgenteRepositoryMySQL implements UbicacionAgenteRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public UbicacionAgenteRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public void guardar(UbicacionAgente ubicacion) {
        jdbc.update(
            """
            INSERT INTO ubicaciones_agente
              (agente_id, incidente_id, latitud, longitud, timestamp)
            VALUES (?, ?, ?, ?, ?)
            """,
            ubicacion.getAgenteId(),
            ubicacion.getIncidenteId(),
            ubicacion.getUbicacion().getLatitud(),
            ubicacion.getUbicacion().getLongitud(),
            ubicacion.getTimestamp()
        );
    }
 
    @Override
    public List<UbicacionAgente> buscarPorIncidente(String incidenteId) {
        return jdbc.query(
            """
            SELECT agente_id, incidente_id, latitud, longitud, timestamp
            FROM ubicaciones_agente
            WHERE incidente_id = ?
            ORDER BY timestamp ASC
            """,
            (rs, i) -> new UbicacionAgente(
                rs.getString("agente_id"),
                rs.getString("incidente_id"),
                new Ubicacion(rs.getDouble("latitud"), rs.getDouble("longitud"))
            ),
            incidenteId
        );
    }
 
    @Override
    public Optional<UbicacionAgente> ultimaPosicion(String agenteId, String incidenteId) {
        return jdbc.query(
            """
            SELECT agente_id, incidente_id, latitud, longitud, timestamp
            FROM ubicaciones_agente
            WHERE agente_id = ? AND incidente_id = ?
            ORDER BY timestamp DESC
            LIMIT 1
            """,
            rs -> rs.next()
                ? Optional.of(new UbicacionAgente(
                    rs.getString("agente_id"),
                    rs.getString("incidente_id"),
                    new Ubicacion(rs.getDouble("latitud"), rs.getDouble("longitud"))))
                : Optional.empty(),
            agenteId, incidenteId
        );
    }
}
