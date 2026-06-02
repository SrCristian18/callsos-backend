/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denuncia;
import java.util.Optional;
 
/**
 * Puerto de salida: persistencia de Denuncia.
 *
 * La tabla denuncias existía en schema.sql pero nunca tuvo adaptador.
 * Sin esto, las denuncias se creaban en memoria y no se persistían.
 *
 * Orden de inserción obligatorio para evitar FK violations:
 *   1. denunciantes  (ya existe al crear el incidente)
 *   2. incidentes    (ya existe al crear el incidente)
 *   3. denuncias     ← requiere que 1 y 2 existan en BD
 */
public interface DenunciaRepositoryPort {
    
    void guardar(Denuncia denuncia);
 
    Optional<Denuncia> buscarPorIncidente(String incidenteId);
}
