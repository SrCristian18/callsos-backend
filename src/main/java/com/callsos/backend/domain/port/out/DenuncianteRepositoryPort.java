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
     * Épica 8 (hallazgo #6, Parte 2): busca un denunciante por su correo
     * — primer paso del flujo de recuperación de contraseña (encontrar
     * a qué actorId corresponde el correo ingresado). No lanza si no
     * hay coincidencia; retorna Optional.empty().
     */
    Optional<Denunciante> buscarPorCorreo(String correo);
    
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

    /** Persiste un denunciante nuevo (registro). */
    void guardar(Denunciante denunciante);

    /** true si ya existe un denunciante con ese documento — evita duplicados. */
    boolean existePorDocumento(String documento);
}