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
import java.util.Optional;
 
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
     * Intenta reservar (DISPONIBLE -> OCUPADO) un agente de forma ATÓMICA
     * a nivel de base de datos.
     *
     * FIX (Épica 4 — condición de carrera en AsignarAgenteService): antes,
     * el flujo era SELECT (obtenerDisponiblesPorUnidad) seguido de un
     * UPDATE ciego (actualizarEstado) sin ninguna condición ni lock entre
     * medio. Dos operadores asignando al mismo tiempo podían leer el mismo
     * agente "disponible" antes de que cualquiera de los dos UPDATE
     * aplicara, resultando en el mismo agente asignado a dos incidentes.
     *
     * Este método reemplaza ese UPDATE ciego por un UPDATE CONDICIONAL
     * (WHERE estado = 'DISPONIBLE'), que es atómico por naturaleza del
     * motor de BD: si dos transacciones lo ejecutan concurrentemente para
     * el mismo agente, solo UNA puede afectar la fila (la otra ve 0 filas
     * afectadas porque para cuando su UPDATE corre, el estado ya cambió).
     * No requiere SELECT ... FOR UPDATE ni un nivel de aislamiento especial
     * — funciona igual en MySQL y H2.
     *
     * @return true si este llamador ganó la reserva (el agente pasó de
     *         DISPONIBLE a OCUPADO); false si alguien más lo reservó
     *         primero (0 filas afectadas) — el llamador debe intentar con
     *         otro candidato, no asumir que el agente sigue disponible.
     */
    boolean intentarReservar(String agenteId);

    /**
     * Persiste un agente nuevo (registro vía invitación).
     * unidadPolicialId se pasa aparte porque el agregado Agente no lo
     * mantiene como campo propio — solo existe como columna FK en BD.
     */
    void guardar(Agente agente, String unidadPolicialId);

    /**
     * Resuelve a qué unidad policial (CAI) pertenece un agente.
     *
     * Épica 3: necesario para que VerificarAccesoTrackingService pueda
     * decidir si un OPERADOR_CAI tiene autorización para ver el tracking
     * de un agente concreto — el dominio Agente no carga esta relación
     * en memoria (solo existe como columna FK en la tabla `agentes`, ver
     * 01_schema.sql), así que hay que consultarla explícitamente.
     *
     * @return Optional.empty() si el agente no existe.
     */
    Optional<String> buscarUnidadDeAgente(String agenteId);

    /**
     * Épica 5 — registra/actualiza el token FCM del agente para
     * notificaciones push (ej. "el denunciante cambió el tipo del
     * incidente"). Mismo propósito y forma que
     * DenuncianteRepositoryPort.actualizarTokenFcm().
     */
    void actualizarTokenFcm(String agenteId, String tokenFcm);
}