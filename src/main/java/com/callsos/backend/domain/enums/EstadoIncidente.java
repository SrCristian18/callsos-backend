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
 * Ciclo de vida completo de un incidente.
 *
 * Máquina de estados:
 *
 *   CREADO
 *     │  (Comando deriva al CAI más cercano)
 *     ▼
 *   DERIVADO_A_CAI
 *     │  (CAI asigna agente disponible)
 *     ▼
 *   AGENTE_ASIGNADO
 *     │  (agente acepta y sale hacia el lugar)
 *     ▼
 *   AGENTE_EN_CAMINO          ← nuevo: necesario para tracking en tiempo real
 *     │  (agente llega y comienza atención)
 *     ▼
 *   EN_ATENCION
 *     │  (agente finaliza y envía reporte)
 *     ▼
 *   FINALIZADO
 *
 * Desde cualquier estado activo el denunciante puede:
 *     → CANCELADO              ← nuevo: denunciante cancela la solicitud
 *
 * NOTA: ASIGNADO se mantiene como alias semántico de DERIVADO_A_CAI
 * para no romper datos existentes en BD hasta la migración.
 */
public enum EstadoIncidente {
    CREADO,
    DERIVADO_A_CAI,
    AGENTE_ASIGNADO,
    AGENTE_EN_CAMINO,
    EN_ATENCION,
    FINALIZADO,
    CANCELADO
}
