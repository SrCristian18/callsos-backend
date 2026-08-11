/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import java.util.Optional;
/**
 * Puerto de salida: buscar credenciales de usuario para autenticación.
 *
 * UsuarioCredencial es un record interno — objeto de valor simple
 * que transporta los datos de autenticación desde la BD al caso de uso.
 * No es un modelo de dominio completo porque la autenticación es
 * infraestructura, no negocio.
 */
public interface UsuarioRepositoryPort {
    
    Optional<UsuarioCredencial> buscarPorUsername(String username);

    /** true si ya existe un usuario con ese username — evita duplicados. */
    boolean existePorUsername(String username);

    /**
     * Persiste un usuario nuevo (registro).
     * El password ya debe venir hasheado con BCrypt — este puerto no hashea.
     */
    void guardar(String id, String username, String nombre, String passwordHash,
                 String rol, String actorId);
 
    /**
     * Datos de autenticación de un usuario.
     *
     * @param id        ID del registro en tabla usuarios
     * @param username  Nombre de usuario para login
     * @param nombre    Nombre para mostrar (Gap 4 — nullable en usuarios
     *                  semilla previos a este fix; UI hace fallback a
     *                  placeholder si viene null)
     * @param password  Hash BCrypt almacenado en BD
     * @param rol       Rol del sistema (DENUNCIANTE, AGENTE, etc.)
     * @param actorId   ID del modelo de negocio asociado
     *                  (denunciante_id, agente_id, unidad_policial_id)
     */
    record UsuarioCredencial(
        String id,
        String username,
        String nombre,
        String password,
        String rol,
        String actorId
    ) {}
}