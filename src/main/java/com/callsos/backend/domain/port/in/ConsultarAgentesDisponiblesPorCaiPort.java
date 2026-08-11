/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

import com.callsos.backend.domain.model.Agente;

import java.util.List;

/**
 * Puerto de entrada: listar agentes DISPONIBLES de un CAI (unidad policial).
 *
 * Resuelve el Gap 3 de deuda_backend.md: hasta ahora, la única forma de
 * conocer los agentes disponibles de un CAI era la lógica interna de
 * AsignarAgenteService (auto-asignación al primer agente DISPONIBLE),
 * sin ningún endpoint que expusiera ese listado hacia afuera.
 *
 * Usado por OPERADOR_CAI para ver — antes de confirmar — a qué agente(s)
 * se podría asignar un incidente derivado a su CAI.
 */
public interface ConsultarAgentesDisponiblesPorCaiPort {
    List<Agente> ejecutar(String caiId);
}
