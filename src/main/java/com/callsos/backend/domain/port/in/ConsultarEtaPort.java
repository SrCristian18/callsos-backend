/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.EtaInfo;

/**
 * Puerto de entrada: consulta bajo demanda del ETA de un incidente
 * (Épica 4). Complementa el broadcast periódico por WebSocket
 * (/topic/incidente/{id}/eta, ver PublicarUbicacionAgenteService) para
 * el caso de reconexión: la app del denunciante puede pedir el valor
 * actual sin esperar a la próxima actualización GPS del agente.
 *
 * actorId se pasa explícitamente (no se resuelve del SecurityContext
 * aquí) siguiendo el mismo patrón que ActualizarTipoIncidentePort —
 * el adaptador de entrada (controller) es quien extrae el actorId del
 * JWT, el puerto de dominio no conoce Spring Security.
 */
public interface ConsultarEtaPort {

    EtaInfo consultar(String incidenteId, String actorId);
}