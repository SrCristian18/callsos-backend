/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Incidente;

import java.util.List;

/**
 * Puerto de entrada: listar incidentes por estado.
 *
 * Usado por COMANDO para obtener todos los incidentes en estado CREADO
 * (pendientes de derivar a un CAI), sin filtrar por actorId — Comando
 * tiene visibilidad de todos los incidentes del sistema.
 *
 * FIX (validación end-to-end): este caso de uso resuelve el gap
 * documentado en deuda_backend.md Gap 2: ninguno de los endpoints
 * existentes (mis-incidentes, asignados, por-cai) era compatible
 * con el rol COMANDO para listar incidentes CREADO.
 */
public interface ConsultarIncidentesPorEstadoPort {
    List<Incidente> ejecutar(EstadoIncidente estado);
}
