package com.callsos.backend.infrastructure.adapter.out.persistence;

import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class ReporteHallazgosRepositoryMySQL implements ReporteHallazgosRepositoryPort {

    private final JdbcTemplate jdbc;

    public ReporteHallazgosRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void guardar(ReporteHallazgos reporte) {
        // FIX (Épica 4): "ON DUPLICATE KEY UPDATE" (MySQL-only) rompía el
        // parser de H2 en @JdbcTest. Reemplazado por UPDATE-si-existe /
        // INSERT-si-no — SQL portátil, mismo patrón que
        // IncidenteRepositoryMySQL.guardar().
        int filas = jdbc.update(
            "UPDATE reportes_hallazgos SET descripcion = ? WHERE id = ?",
            reporte.getDescripcion(),
            reporte.getId()
        );

        if (filas == 0) {
            jdbc.update("""
                INSERT INTO reportes_hallazgos
                  (id, fecha, descripcion, incidente_id, agente_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                reporte.getId(),
                reporte.getFecha(),
                reporte.getDescripcion(),
                reporte.getIncidente().getId(),
                reporte.getAgente().getId()
            );
        }
    }

    /**
     * FIX: antes retornaba siempre List.of() — deuda técnica documentada.
     *
     * Ahora hace JOIN con incidentes y agentes para reconstituir los reportes.
     * Se usa Agente sin UnidadPolicial (JOIN parcial) porque en el contexto
     * de consulta de reportes no se necesita la jerarquía completa.
     */
    @Override
    public List<ReporteHallazgos> buscarPorIncidente(String incidenteId) {
        return jdbc.query("""
            SELECT rh.id, rh.fecha, rh.descripcion,
                   rh.incidente_id,
                   i.tipo           AS i_tipo,
                   i.descripcion    AS i_desc,
                   i.estado         AS i_estado,
                   i.latitud        AS i_lat,
                   i.longitud       AS i_lon,
                   i.denunciante_id AS i_den_id,
                   d.nombre         AS d_nombre,
                   d.origen         AS d_origen,
                   d.telefono       AS d_tel,
                   d.correo         AS d_correo,
                   rh.agente_id,
                   ag.nombre        AS ag_nombre,
                   ag.direccion     AS ag_dir,
                   ag.telefono      AS ag_tel,
                   ag.latitud       AS ag_lat,
                   ag.longitud      AS ag_lon
            FROM reportes_hallazgos rh
            JOIN incidentes i    ON i.id  = rh.incidente_id
            JOIN denunciantes d  ON d.id  = i.denunciante_id
            JOIN agentes ag      ON ag.id = rh.agente_id
            WHERE rh.incidente_id = ?
            ORDER BY rh.fecha DESC
            """,
            (rs, rowNum) -> {
                Denunciante denunciante = new Denunciante(
                    rs.getString("i_den_id"),
                    rs.getString("d_nombre"),
                    rs.getString("d_origen"),
                    rs.getString("d_tel"),
                    rs.getString("d_correo")
                );
                Incidente incidente = new Incidente(
                    rs.getString("incidente_id"),
                    TipoIncidente.valueOf(rs.getString("i_tipo")),
                    rs.getString("i_desc"),
                    new Ubicacion(rs.getDouble("i_lat"), rs.getDouble("i_lon")),
                    denunciante
                );
                incidente.reconstituirEstado(
                    EstadoIncidente.valueOf(rs.getString("i_estado")));

                double agLat = rs.getDouble("ag_lat");
                double agLon = rs.getDouble("ag_lon");
                Agente agente = new Agente(
                    rs.getString("agente_id"),
                    rs.getString("ag_nombre"),
                    rs.getString("ag_dir"),
                    (agLat == 0 && agLon == 0) ? null : new Ubicacion(agLat, agLon),
                    rs.getString("ag_tel")
                );

                return new ReporteHallazgos(
                    rs.getString("id"),
                    rs.getString("descripcion"),
                    incidente,
                    agente
                );
            },
            incidenteId
        );
    }
}