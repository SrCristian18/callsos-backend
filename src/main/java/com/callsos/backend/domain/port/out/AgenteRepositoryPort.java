/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Agente;
 
import java.util.List;
 
/**
 * Puerto de salida: contrato de persistencia para Agente.
 */
public interface AgenteRepositoryPort {
    
    /** Todos los agentes en estado DISPONIBLE (cualquier unidad). */
    List<Agente> obtenerDisponibles();
 
    /**
     * Agentes DISPONIBLES filtrados por unidad policial.
     *
     * Se agrega para corregir el bug de AsignarAgenteService:
     * el filtro se hace en SQL por ID, no en memoria por referencia Java.
     */
    List<Agente> obtenerDisponiblesPorUnidad(String unidadPolicialId);
 
    void actualizarEstado(Agente agente);

    /**
     * Persiste un agente nuevo (registro vía invitación).
     * unidadPolicialId se pasa aparte porque el agregado Agente no lo
     * mantiene como campo propio — solo existe como columna FK en BD.
     */
    void guardar(Agente agente, String unidadPolicialId);
}