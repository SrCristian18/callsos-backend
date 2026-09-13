/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 * Puerto de salida: generación del token de sesión entregado al actor tras
 * un login o registro exitoso.
 *
 * FIX (auditoría AUD-arquitectura): antes de este puerto,
 * {@code LoginService}, {@code RegistrarDenuncianteService} y
 * {@code RegistrarAgenteConInvitacionService} (todos en
 * {@code application/service}) importaban y dependían directamente de
 * {@code JwtService} (clase concreta en
 * {@code infrastructure.config.security}) — una violación real de la regla
 * de dependencia hexagonal: la capa de aplicación conocía un detalle de
 * infraestructura (JWT, JJWT, HMAC) en vez de depender de una abstracción
 * propia del dominio.
 *
 * No expone nada específico de JWT (ni "claims", ni "expiración", ni
 * ningún tipo de la librería JJWT) — solo el contrato que la aplicación
 * realmente necesita: te doy quién es el actor y su rol, te devuelvo un
 * token opaco. Los detalles de CÓMO se genera ese token (algoritmo,
 * secreto, tiempo de expiración) son responsabilidad exclusiva del
 * adaptador que implemente este puerto ({@code JwtService}).
 */
public interface TokenGeneratorPort {

    /**
     * Genera un token de sesión para el actor autenticado.
     *
     * @param actorId identificador del actor (denunciante, agente, unidad
     *                policial o comando) — se vuelve el "subject" del
     *                token.
     * @param rol     nombre del rol ({@link com.callsos.backend.domain.enums.RolUsuario})
     *                asociado a esa sesión.
     * @return token de sesión opaco, listo para devolver al cliente.
     */
    String generarToken(String actorId, String rol);
}