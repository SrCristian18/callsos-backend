/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denunciante;
 
import java.util.Optional;

/**
 * Puerto de salida: contrato de persistencia para Denunciante.
 * CrearIncidenteService lo usa para recuperar el denunciante por ID.
 */
public interface DenuncianteRepositoryPort {
    
    Optional<Denunciante> buscarPorId(String id);
    
    /**
     * Actualiza el token FCM del denunciante en BD.
     *
     * Firebase renueva los tokens periódicamente.
     * Flutter debe llamar a PATCH /api/v1/denunciantes/{id}/token
     * cada vez que reciba un token nuevo del SDK de Firebase,
     * para que el backend siempre tenga el token vigente.
     *
     * @param denuncianteId  ID del denunciante autenticado
     * @param tokenFcm       Nuevo token emitido por Firebase en el dispositivo
     */
    void actualizarTokenFcm(String denuncianteId, String tokenFcm);
}
