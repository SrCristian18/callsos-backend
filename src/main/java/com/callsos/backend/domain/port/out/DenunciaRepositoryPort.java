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
import com.callsos.backend.domain.model.Incidente;
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
 
    /**
     * Busca la Denuncia asociada a un Incidente.
     *
     * FIX (validación end-to-end): recibe el [incidente] YA RECONSTRUIDO
     * como parámetro (en vez de reconstruirlo internamente) para resolver
     * la dependencia circular Denuncia <-> Incidente sin recursión
     * infinita — el llamador (IncidenteRepositoryMySQL.mapRow) ya tiene el
     * Incidente armado en memoria cuando necesita completar su Denuncia.
     */
    Optional<Denuncia> buscarPorIncidente(String incidenteId, Incidente incidente);
}