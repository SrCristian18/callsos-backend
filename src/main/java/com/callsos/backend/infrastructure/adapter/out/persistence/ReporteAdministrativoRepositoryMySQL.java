/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.ReporteAdministrativo;
import com.callsos.backend.domain.port.out.ReporteAdministrativoRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
 
import javax.sql.DataSource;
import java.util.List;
 
@Component
public class ReporteAdministrativoRepositoryMySQL implements ReporteAdministrativoRepositoryPort {
    
    private final JdbcTemplate jdbc;
 
    public ReporteAdministrativoRepositoryMySQL(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
 
    @Override
    public void guardar(ReporteAdministrativo reporte) {
        // FIX (Épica 4): "ON DUPLICATE KEY UPDATE" (MySQL-only) rompía el
        // parser de H2 en @JdbcTest. Reemplazado por UPDATE-si-existe /
        // INSERT-si-no — SQL portátil, mismo patrón que
        // IncidenteRepositoryMySQL.guardar().
        int filas = jdbc.update(
            "UPDATE reportes_administrativos SET resumen = ? WHERE id = ?",
            reporte.getResumen(),
            reporte.getId()
        );

        if (filas == 0) {
            jdbc.update(
                """
                INSERT INTO reportes_administrativos
                  (id, fecha, resumen, incidente_id, autoridad_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                reporte.getId(),
                reporte.getFecha(),
                reporte.getResumen(),
                reporte.getIncidente().getId(),
                reporte.getAutoridad().getId()
            );
        }
    }
 
    @Override
    public List<ReporteAdministrativo> buscarPorIncidente(String incidenteId) {
        return List.of(); // reconstitución pendiente Fase 2
    }
}