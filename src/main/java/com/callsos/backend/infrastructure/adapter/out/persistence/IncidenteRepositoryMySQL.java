package com.callsos.backend.infrastructure.adapter.out.persistence;

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
import java.util.List;
import java.util.Optional;

@Component
public class IncidenteRepositoryMySQL implements IncidenteRepositoryPort {

    private final JdbcTemplate jdbc;

    public IncidenteRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void guardar(Incidente incidente) {
        String unidadId = (incidente.getUnidadPolicial() != null)
            ? incidente.getUnidadPolicial().getId() : null;

        int filas = jdbc.update("""
            UPDATE incidentes
            SET descripcion = ?, estado = ?, latitud = ?,
                longitud = ?, unidad_policial_id = ?
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

    @Override
    public Optional<Incidente> buscarPorId(String id) {
        return jdbc.query(BASE_SQL + " WHERE i.id = ?",
            rs -> {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }, id);
    }

    @Override
    public void actualizarEstado(String id, EstadoIncidente estado) {
        jdbc.update("UPDATE incidentes SET estado = ? WHERE id = ?",
            estado.name(), id);
    }

    /**
     * Historial del denunciante — todos sus incidentes ordenados por fecha descendente.
     * Permite al denunciante ver el historial completo en "Mis denuncias".
     */
    @Override
    public List<Incidente> buscarPorDenunciante(String denuncianteId) {
        return jdbc.query(
            BASE_SQL + " WHERE i.denunciante_id = ? ORDER BY i.fecha_hora DESC",
            new IncidenteRowMapper(), denuncianteId);
    }

    /**
     * Cola del agente — incidentes activos asignados a él.
     * Busca en la tabla asignaciones los incidentes ACTIVOS del agente,
     * filtrando por estados operativos (no finalizados ni cancelados).
     */
    @Override
    public List<Incidente> buscarAsignadosAlAgente(String agenteId) {
        String sql = BASE_SQL + """
             JOIN asignaciones a ON a.incidente_id = i.id
            WHERE a.agente_id = ?
              AND a.estado = 'ACTIVA'
              AND i.estado NOT IN ('FINALIZADO', 'CANCELADO')
            ORDER BY i.fecha_hora DESC
            """;
        return jdbc.query(sql, new IncidenteRowMapper(), agenteId);
    }

    /**
     * Panel del CAI — incidentes activos de la unidad policial.
     * Excluye FINALIZADO y CANCELADO para mostrar solo la carga operativa actual.
     */
    @Override
    public List<Incidente> buscarPorCAI(String unidadPolicialId) {
        return jdbc.query(
            BASE_SQL + """
             WHERE i.unidad_policial_id = ?
               AND i.estado NOT IN ('FINALIZADO', 'CANCELADO')
             ORDER BY i.fecha_hora DESC
            """,
            new IncidenteRowMapper(), unidadPolicialId);
    }

    // ── SQL base reutilizable ─────────────────────────────────────────────────

    private static final String BASE_SQL = """
        SELECT i.id, i.fecha_hora, i.tipo, i.descripcion, i.estado,
               i.latitud, i.longitud,
               i.denunciante_id,
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
        """;

    // ── Mapper compartido ─────────────────────────────────────────────────────

    private static Incidente mapRow(ResultSet rs) throws SQLException {
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
        incidente.reconstituirEstado(EstadoIncidente.valueOf(rs.getString("estado")));
        String unidadId = rs.getString("unidad_policial_id");
        if (unidadId != null) {
            incidente.reconstituirUnidad(new UnidadPolicial(
                unidadId,
                rs.getString("u_nombre"),
                rs.getString("u_direccion"),
                new Ubicacion(rs.getDouble("u_latitud"), rs.getDouble("u_longitud")),
                rs.getString("u_telefono")
            ));
        }
        return incidente;
    }

    private static class IncidenteRowMapper implements RowMapper<Incidente> {
        @Override
        public Incidente mapRow(ResultSet rs, int rowNum) throws SQLException {
            return IncidenteRepositoryMySQL.mapRow(rs);
        }
    }
}