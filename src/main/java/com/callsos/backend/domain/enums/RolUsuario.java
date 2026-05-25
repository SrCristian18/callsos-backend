/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.callsos.backend.domain.enums;

/**
 *
 * @author LENOVO
 */
/**
 * Roles del sistema. Determinan qué endpoints puede acceder cada actor.
 *
 * DENUNCIANTE   → crear incidentes, consultar los suyos, cancelar
 * AGENTE        → ver incidentes asignados, marcar en camino, atender, reportar
 * OPERADOR_CAI  → asignar agentes, ver incidentes del CAI
 * COMANDO       → acceso total, reportes administrativos, estadísticas
 */
public enum RolUsuario {
    DENUNCIANTE,
    AGENTE,
    OPERADOR_CAI,
    COMANDO
}
