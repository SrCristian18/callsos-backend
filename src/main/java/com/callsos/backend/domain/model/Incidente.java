/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import lombok.Getter;
 
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
/**
 * Agregado raíz del dominio.
 * Representa un hecho delictivo o de orden público que requiere atención.
 *
 * Ciclo de vida del estado:
 *   CREADO → ASIGNADO → EN_PROCESO → FINALIZADO
 *
 * Invariantes:
 *   - Solo se puede asignar una unidad si el estado es CREADO.
 *   - Solo se puede cambiar de estado en el orden establecido.
 *   - La lista de asignaciones es inmutable desde fuera del agregado.
 */
@Getter
public class Incidente {
 
    private final String id;                    // UUID
    private final LocalDateTime fechaHora;
    private final TipoIncidente tipo;
    private String descripcion;
    private EstadoIncidente estado;
    private final Ubicacion ubicacion;
    private final Denunciante denunciante;
    private UnidadPolicial unidadPolicial;      // CAI asignada
    private Denuncia denuncia;                  // Denuncia que originó este incidente
    
    private final List<Asignacion> asignaciones;
 
    public Incidente(String id, TipoIncidente tipo, String descripcion,
                     Ubicacion ubicacion, Denunciante denunciante) {
        this.id           = id;
        this.tipo         = tipo;
        this.descripcion  = descripcion;
        this.ubicacion    = ubicacion;
        this.denunciante  = denunciante;
        this.fechaHora    = LocalDateTime.now();
        this.estado       = EstadoIncidente.CREADO;
        this.asignaciones = new ArrayList<>();
    }
 
    // ── Comportamiento de dominio ──────────────────────────────────────────
 
    /**
     * Asigna un CAI (UnidadPolicial) al incidente.
     * Solo permitido en estado CREADO.
     */
    public void asignarCAI(UnidadPolicial cai) {
        if (!EstadoIncidente.CREADO.equals(this.estado))
            throw new IllegalStateException(
                "Solo se puede asignar un CAI a un incidente en estado CREADO.");
        this.unidadPolicial = cai;
        this.estado = EstadoIncidente.ASIGNADO;
    }
 
    /**
     * Avanza el estado del incidente al siguiente en el ciclo de vida.
     * ASIGNADO → EN_PROCESO → FINALIZADO
     */
    public void cambiarEstado(EstadoIncidente nuevoEstado) {
        validarTransicion(this.estado, nuevoEstado);
        this.estado = nuevoEstado;
    }
 
    /**
     * Registra una nueva asignación de agente a este incidente.
     */
    public void agregarAsignacion(Asignacion asignacion) {
        if (asignacion == null)
            throw new IllegalArgumentException("La asignación no puede ser nula.");
        if (EstadoIncidente.FINALIZADO.equals(this.estado))
            throw new IllegalStateException(
                "No se pueden agregar asignaciones a un incidente finalizado.");
        this.asignaciones.add(asignacion);
    }
 
    public Denuncia getDenuncia() { return denuncia; }
 
    /** Vincula la Denuncia que originó este incidente. Solo puede asignarse una vez. */
    public void setDenuncia(Denuncia denuncia) {
        if (this.denuncia != null)
            throw new IllegalStateException("El incidente ya tiene una Denuncia vinculada.");
        this.denuncia = denuncia;
    }
    
    /** Vista inmutable de las asignaciones. */
    public List<Asignacion> getAsignaciones() {
        return Collections.unmodifiableList(asignaciones);
    }
 
    // ── Lógica de transición ───────────────────────────────────────────────
 
    private void validarTransicion(EstadoIncidente actual, EstadoIncidente siguiente) {
        boolean valida = switch (actual) {
            case CREADO     -> siguiente == EstadoIncidente.ASIGNADO;
            case ASIGNADO   -> siguiente == EstadoIncidente.EN_PROCESO;
            case EN_PROCESO -> siguiente == EstadoIncidente.FINALIZADO;
            case FINALIZADO -> false;
        };
        if (!valida)
            throw new IllegalStateException(
                "Transición inválida: " + actual + " → " + siguiente);
    }
}