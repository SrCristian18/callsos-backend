/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 * Puerto de entrada: decide si un actor puede acceder al tracking GPS
 * (en tiempo real o histórico) de un agente concreto (Épica 3 — fix P6).
 *
 * Matriz de autorización (ver análisis técnico, sección 18):
 *   - AGENTE:       solo su propio tracking (agenteId == actorId).
 *   - OPERADOR_CAI: solo agentes de su propia unidad.
 *   - COMANDO:      acceso global, sin restricción.
 *   - DENUNCIANTE
 *     y cualquier otro rol: SIEMPRE denegado — esta es la regla de
 *     seguridad central de la Épica 3: el denunciante nunca puede ver
 *     la ubicación del agente, bajo ninguna circunstancia.
 *
 * Usado por StompAuthChannelInterceptor para autorizar SUBSCRIBE a
 * /topic/agente/{agenteId}/ubicacion — la autorización real de acceso
 * vive acá, no en el frontend ("ocultar el mapa" en Flutter no alcanza).
 */
public interface VerificarAccesoTrackingPort {

    /**
     * @param agenteId ID del agente cuyo tracking se quiere consultar.
     * @param actorId  ID del actor autenticado (del JWT).
     * @param rol      Rol del actor autenticado, sin el prefijo "ROLE_".
     * @return true si el actor puede ver el tracking de ese agente.
     */
    boolean puedeAcceder(String agenteId, String actorId, String rol);
}
