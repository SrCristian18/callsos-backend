/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.ReporteHallazgos;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
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
        String sql = """
            INSERT INTO reportes_hallazgos
              (id, fecha, descripcion, incidente_id, agente_id)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion)
            """;
        jdbc.update(sql,
            reporte.getId(),
            reporte.getFecha(),
            reporte.getDescripcion(),
            reporte.getIncidente().getId(),
            reporte.getAgente().getId()
        );
    }
 
    @Override
    public List<ReporteHallazgos> buscarPorIncidente(String incidenteId) {
        // Reconstitución completa pendiente (requiere joins y constructor de reconstitución)
        // Fase 1: solo retorna lista vacía — suficiente para que el flujo funcione
        return List.of();
    }
}
