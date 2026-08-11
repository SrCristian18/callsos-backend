/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.port.out.DenunciaRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
 
/**
 * Adaptador de salida: persiste Denuncia en la tabla denuncias.
 *
 * IMPORTANTE — orden de persistencia en el servicio que llame a este adaptador:
 *   1. incidenteRepository.guardar(incidente)  ← primero
 *   2. denunciaRepository.guardar(denuncia)    ← después (FK sobre incidente_id)
 */
@Component
public class DenunciaRepositoryMySQL implements DenunciaRepositoryPort{
    
    private final JdbcTemplate jdbc;
 
    public DenunciaRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public void guardar(Denuncia denuncia) {
        // FIX (Épica 4): "INSERT ... ON DUPLICATE KEY UPDATE" es sintaxis
        // exclusiva de MySQL — H2 (usado en los tests @JdbcTest) no puede
        // ni siquiera PARSEARLA, así que fallaba con BadSqlGrammarException
        // en el primer INSERT, no solo en el caso de duplicado. Se
        // reemplaza por UPDATE-si-existe / INSERT-si-no, el mismo patrón
        // ya usado en IncidenteRepositoryMySQL.guardar() — SQL portátil,
        // sin extensiones de proveedor, mismo comportamiento neto en MySQL.
        int filas = jdbc.update(
            "UPDATE denuncias SET descripcion = ? WHERE id = ?",
            denuncia.getDescripcion(),
            denuncia.getId()
        );

        if (filas == 0) {
            jdbc.update(
                """
                INSERT INTO denuncias
                  (id, fecha, tipo, descripcion, latitud, longitud,
                   denunciante_id, incidente_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                denuncia.getId(),
                denuncia.getFecha(),
                denuncia.getTipo().name(),
                denuncia.getDescripcion(),
                denuncia.getUbicacion() != null ? denuncia.getUbicacion().getLatitud()  : null,
                denuncia.getUbicacion() != null ? denuncia.getUbicacion().getLongitud() : null,
                denuncia.getDenunciante().getId(),
                denuncia.getIncidente().getId()
            );
        }
    }
 
    @Override
    public Optional<Denuncia> buscarPorIncidente(String incidenteId,
            com.callsos.backend.domain.model.Incidente incidente) {
        // FIX (validación end-to-end): antes este método era un stub que
        // devolvía Optional.empty() siempre ("Reconstitución completa
        // pendiente — requeriría JOIN circular"). Esto rompía
        // AsignarAgenteService, que exige incidente.getDenuncia() != null
        // — ningún incidente real (creado vía CrearIncidenteService) podía
        // pasar de DERIVADO_A_CAI a AGENTE_ASIGNADO.
        //
        // El JOIN circular se resuelve recibiendo el Incidente YA
        // RECONSTRUIDO como parámetro (lo arma el llamador,
        // IncidenteRepositoryMySQL.mapRow) y usando Denuncia.reconstituir()
        // (sin validación de "incidente nuevo", sin fecha=now()).
        String sql = """
            SELECT den.id, den.fecha, den.tipo, den.descripcion,
                   den.latitud, den.longitud,
                   dn.id          AS dn_id,
                   dn.nombre      AS dn_nombre,
                   dn.origen      AS dn_origen,
                   dn.telefono    AS dn_telefono,
                   dn.correo      AS dn_correo,
                   dn.token_fcm   AS dn_token_fcm
            FROM denuncias den
            JOIN denunciantes dn ON dn.id = den.denunciante_id
            WHERE den.incidente_id = ?
            """;

        List<Denuncia> resultado = jdbc.query(sql, (rs, rowNum) -> {
            var denunciante = new com.callsos.backend.domain.model.Denunciante(
                rs.getString("dn_id"),
                rs.getString("dn_nombre"),
                rs.getString("dn_origen"),
                rs.getString("dn_telefono"),
                rs.getString("dn_correo"),
                rs.getString("dn_token_fcm")
            );

            // FIX ClassCastException: MySQL DECIMAL(10,8) → Java BigDecimal.
            // rs.getDouble() hace la conversión automáticamente; getObject()
            // devuelve BigDecimal que no es casteable a Double directamente.
            double latVal = rs.getDouble("latitud");
            double lonVal = rs.getDouble("longitud");
            var ubicacion = (!rs.wasNull())
                ? new com.callsos.backend.domain.valueobject.Ubicacion(latVal, lonVal)
                : null;

            return Denuncia.reconstituir(
                rs.getString("id"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                com.callsos.backend.domain.enums.TipoIncidente.valueOf(rs.getString("tipo")),
                rs.getString("descripcion"),
                ubicacion,
                denunciante,
                incidente
            );
        }, incidenteId);

        return resultado.stream().findFirst();
    }
}