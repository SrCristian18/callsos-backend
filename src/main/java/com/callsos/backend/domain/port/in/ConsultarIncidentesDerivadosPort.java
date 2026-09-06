/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

import com.callsos.backend.domain.model.Incidente;

import java.util.List;

/**
 * Puerto de entrada: historial completo de incidentes ya derivados a un
 * CAI, para COMANDO.
 *
 * EPIC-18 (frontend) / hallazgo #14 de la auditoría UX/UI: resuelve el
 * gap documentado en deuda_backend.md — hasta ahora, el único endpoint
 * de listado disponible para COMANDO era `porEstado(CREADO)` (lo
 * pendiente de derivar); en cuanto un incidente se derivaba, dejaba de
 * ser consultable en cualquier vista de lista de Comando (solo quedaba
 * accesible entrando al detalle uno por uno, si se tenía el ID a mano).
 *
 * Sin restricción de actorId — mismo criterio que
 * {@link ConsultarIncidentesPorEstadoPort}: Comando tiene visibilidad
 * total del sistema, a diferencia de los otros roles.
 */
public interface ConsultarIncidentesDerivadosPort {
    List<Incidente> ejecutar();
}