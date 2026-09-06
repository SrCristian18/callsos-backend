/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.model.Incidente;
 
import java.util.Optional;
import java.util.List;
 
/**
 * Puerto de salida: contrato de persistencia para Incidente.
 *
 * Fase E: se agregan 3 métodos de consulta que Flutter necesita
 * para construir las pantallas de historial y panel de trabajo.
 *
 * Épica 8 (hallazgo #8.2 — limpieza de código muerto): se retiró
 * `actualizarEstado(String, EstadoIncidente)` — nunca tuvo callers
 * reales; todo el flujo de transición de estado pasa por
 * `guardar(Incidente)`, que persiste el agregado completo (incluido
 * su estado) tras que el propio `Incidente` valida la transición
 * (`validarTransicion`). Confirmado sin riesgo antes de eliminar.
 */
public interface IncidenteRepositoryPort {
    
    void guardar(Incidente incidente);
 
    Optional<Incidente> buscarPorId(String id);
 
    /**
     * Historial de incidentes creados por un denunciante.
     * Pantalla: "Mis denuncias" en la app del denunciante.
     *
     * @param denuncianteId  actorId del denunciante (viene del JWT)
     */
    List<Incidente> buscarPorDenunciante(String denuncianteId);
 
    /**
     * Incidentes asignados a un agente específico (estado AGENTE_ASIGNADO,
     * AGENTE_EN_CAMINO o EN_ATENCION).
     * Pantalla: "Mi cola de trabajo" en la app del agente.
     *
     * @param agenteId  actorId del agente (viene del JWT)
     */
    List<Incidente> buscarAsignadosAlAgente(String agenteId);
 
    /**
     * Incidentes activos de una unidad policial (CAI).
     * Pantalla: panel de operaciones del OPERADOR_CAI.
     *
     * @param unidadPolicialId  ID del CAI
     */
    List<Incidente> buscarPorCAI(String unidadPolicialId);

    /**
     * Busca todos los incidentes en un estado dado, sin filtrar por actor.
     * Usado por ConsultarIncidentesPorEstadoPort (caso de uso de COMANDO).
     */
    List<Incidente> buscarPorEstado(EstadoIncidente estado);

    /**
     * Historial completo de derivaciones — todo incidente que en algún
     * momento fue asignado a una UnidadPolicial, sin importar su estado
     * actual (activo, finalizado o cancelado).
     *
     * EPIC-18 (frontend) / hallazgo #14: resuelve el gap documentado en
     * deuda_backend.md — antes de esto, `buscarPorEstado(CREADO)` era lo
     * único que Comando podía consultar; en cuanto un incidente se
     * derivaba, desaparecía de su visibilidad por completo.
     *
     * Filtra por `unidad_policial_id IS NOT NULL` en vez de
     * `estado != CREADO` a propósito: un incidente puede pasar de CREADO
     * a CANCELADO directamente (el denunciante puede cancelar "desde
     * cualquier estado activo" — ver Incidente.cancelar()), SIN haber
     * sido derivado nunca. Ese caso debe seguir apareciendo en
     * "Reportados" (o desaparecer sin más, si se cancela), nunca en
     * "Delegados" — `unidad_policial_id` es el único campo que
     * distingue "llegó a tener un CAI asignado" de "nunca lo tuvo",
     * y una vez seteado (en AsignarCAIAIncidenteService.ejecutar())
     * `guardar()` nunca lo vuelve a limpiar, así que el historial se
     * conserva aunque el incidente termine cancelado o finalizado.
     */
    List<Incidente> buscarDerivados();
}