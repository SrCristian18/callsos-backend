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
        jdbc.update(
            """
            INSERT INTO denuncias
              (id, fecha, tipo, descripcion, latitud, longitud,
               denunciante_id, incidente_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion)
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
 
    @Override
    public Optional<Denuncia> buscarPorIncidente(String incidenteId) {
        // Reconstitución completa pendiente — requeriría JOIN circular.
        // Para verificar existencia usar tieneAsignacionActiva equivalente.
        return Optional.empty();
    }
}
