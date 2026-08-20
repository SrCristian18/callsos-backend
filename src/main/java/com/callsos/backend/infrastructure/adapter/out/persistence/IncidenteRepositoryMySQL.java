package com.callsos.backend.infrastructure.adapter.out.persistence;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.DenunciaRepositoryPort;
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
    // FIX (validación end-to-end): inyectado para resolver
    // incidente.getDenuncia() al reconstruir desde BD — antes mapRow()
    // nunca cargaba la Denuncia asociada, lo que rompía AsignarAgenteService
    // para CUALQUIER incidente real (no solo datos de prueba).
    private final DenunciaRepositoryPort denunciaRepository;

    public IncidenteRepositoryMySQL(DataSource dataSource,
                                    DenunciaRepositoryPort denunciaRepository) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.denunciaRepository = denunciaRepository;
    }

    @Override
    public void guardar(Incidente incidente) {
        String unidadId = (incidente.getUnidadPolicial() != null)
            ? incidente.getUnidadPolicial().getId() : null;

        // FIX (Épica 1): "tipo" faltaba en el SET del UPDATE — solo se
        // escribía en el INSERT inicial. Cualquier cambio de tipo en
        // memoria (ver Incidente.cambiarTipo()) se perdía silenciosamente
        // al persistir, porque guardar() hace upsert por "filas == 0" y
        // el UPDATE nunca tocaba esta columna.
        int filas = jdbc.update("""
            UPDATE incidentes
            SET tipo = ?, descripcion = ?, estado = ?, latitud = ?,
                longitud = ?, unidad_policial_id = ?
            WHERE id = ?
            """,
            incidente.getTipo().name(),
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
        Optional<Incidente> incidente = jdbc.query(BASE_SQL + " WHERE i.id = ?",
            rs -> {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }, id);
        incidente.ifPresent(this::cargarDenuncia);
        return incidente;
    }

    @Override
    public void actualizarEstado(String id, EstadoIncidente estado) {
        jdbc.update("UPDATE incidentes SET estado = ? WHERE id = ?",
            estado.name(), id);
    }

    @Override
    public List<Incidente> buscarPorDenunciante(String denuncianteId) {
        List<Incidente> lista = jdbc.query(
            BASE_SQL + " WHERE i.denunciante_id = ? ORDER BY i.fecha_hora DESC",
            new IncidenteRowMapper(), denuncianteId);
        lista.forEach(this::cargarDenuncia);
        return lista;
    }

    @Override
    public List<Incidente> buscarAsignadosAlAgente(String agenteId) {
        String sql = BASE_SQL + """
             JOIN asignaciones a ON a.incidente_id = i.id
            WHERE a.agente_id = ?
              AND a.estado = 'ACTIVA'
              AND i.estado NOT IN ('FINALIZADO', 'CANCELADO')
            ORDER BY i.fecha_hora DESC
            """;
        List<Incidente> lista = jdbc.query(sql, new IncidenteRowMapper(), agenteId);
        lista.forEach(this::cargarDenuncia);
        return lista;
    }

    @Override
    public List<Incidente> buscarPorCAI(String unidadPolicialId) {
        List<Incidente> lista = jdbc.query(
            BASE_SQL + """
             WHERE i.unidad_policial_id = ?
               AND i.estado NOT IN ('FINALIZADO', 'CANCELADO')
             ORDER BY i.fecha_hora DESC
            """,
            new IncidenteRowMapper(), unidadPolicialId);
        lista.forEach(this::cargarDenuncia);
        return lista;
    }

    // ── Helper: carga la Denuncia DESPUÉS de que el ResultSet principal
    // está cerrado — evita el error de nested JDBC query.
    // Si el incidente no tiene Denuncia en BD (incidentes previos al fix),
    // ifPresent no hace nada y el incidente sigue sin Denuncia. Solo los
    // nuevos incidentes creados post-fix tendrán Denuncia vinculada.
    private void cargarDenuncia(Incidente incidente) {
        denunciaRepository.buscarPorIncidente(incidente.getId(), incidente)
            .ifPresent(incidente::setDenuncia);
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
               u.telefono  AS u_telefono,
               u.token_fcm AS u_token_fcm
        FROM incidentes i
        JOIN denunciantes d ON d.id = i.denunciante_id
        LEFT JOIN unidades_policiales u ON u.id = i.unidad_policial_id
        """;

    // ── Mapper compartido ─────────────────────────────────────────────────────
    // FIX: ya no es estático — necesita denunciaRepository (instancia) para
    // completar incidente.setDenuncia() tras reconstruir el Incidente.
    private Incidente mapRow(ResultSet rs) throws SQLException {
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
            UnidadPolicial unidad = new UnidadPolicial(
                unidadId,
                rs.getString("u_nombre"),
                rs.getString("u_direccion"),
                new Ubicacion(rs.getDouble("u_latitud"), rs.getDouble("u_longitud")),
                rs.getString("u_telefono")
            );
            // Épica 5: el token FCM del CAI viaja con el incidente porque
            // NotificacionEventListener necesita notificar a la unidad
            // policial cuando el denunciante cambia el tipo, y ya tiene
            // el Incidente completo cargado en memoria — evita una
            // consulta extra a UnidadPolicialRepositoryPort.
            unidad.setTokenFcm(rs.getString("u_token_fcm"));
            incidente.reconstituirUnidad(unidad);
        }
        // NOTA: la Denuncia NO se carga aquí — se carga en cargarDenuncia()
        // DESPUÉS de que este ResultSet esté cerrado, para evitar el error
        // de JDBC nested query (segunda query dentro de un ResultSet activo).
        return incidente;
    }

    @Override
    public List<Incidente> buscarPorEstado(EstadoIncidente estado) {
        List<Incidente> lista = jdbc.query(
            BASE_SQL + " WHERE i.estado = ? ORDER BY i.fecha_hora DESC",
            new IncidenteRowMapper(), estado.name());
        lista.forEach(this::cargarDenuncia);
        return lista;
    }

    private class IncidenteRowMapper implements RowMapper<Incidente> {
        @Override
        public Incidente mapRow(ResultSet rs, int rowNum) throws SQLException {
            return IncidenteRepositoryMySQL.this.mapRow(rs);
        }
    }
}