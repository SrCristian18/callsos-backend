/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.valueobject.Ubicacion;
import lombok.Getter;
 
import java.util.List;
 
/**
 * Nodo compuesto del patrón Composite.
 *
 * Una UnidadPolicial (CAI) puede contener Agentes (hojas) u otras
 * UnidadesPolicial (nodos), formando una jerarquía arbitraria.
 * La lista de subordinados es gestionada por AutoridadPolicial.
 *
 * La agregación (rombo blanco) del diagrama 2 está representada
 * estructuralmente por la herencia de AutoridadPolicial, que
 * posee internamente List<AutoridadPolicial>.
 */
public class UnidadPolicial extends AutoridadPolicial {
 
 
    public UnidadPolicial(String id, String nombre,
                          String direccion, Ubicacion ubicacion,
                          String telefono) {
        super(id, nombre, direccion, ubicacion, telefono);
    }
 
    @Override
    public String getRol() {
        return "UNIDAD_POLICIAL";
    }
 
    // ── Consultas convenientes ─────────────────────────────────────────────
 
    /**
     * Devuelve solo los Agentes directamente subordinados a esta unidad.
     *
     * NOTA (Épica 8, hallazgo #8.1): en la práctica `subordinados` (la
     * lista del Composite heredada de {@link AutoridadPolicial}) está
     * siempre vacía — ningún caso de uso real puebla la jerarquía en
     * memoria. La consulta REAL de "agentes de un CAI" que usa el resto
     * de la app va directo a BD vía
     * `AgenteRepositoryPort.obtenerDisponiblesPorUnidad()`
     * (`ConsultarAgentesDisponiblesPorCaiService`), no por acá. Este
     * método queda funcional para cuando el Composite se empiece a
     * poblar de verdad (ver docstring de {@link AutoridadPolicial}).
     */
    public List<Agente> getAgentes() {
       return getSubordinados().stream()
            .filter(s -> s instanceof Agente)
            .map(s -> (Agente) s)
            .toList();
    }
    
    /**
     * Devuelve los agentes disponibles en esta unidad.
     */
    public List<Agente> getAgentesDisponibles() {
        return getAgentes().stream()
            .filter(Agente::estaDisponible)
            .toList();
    }
}