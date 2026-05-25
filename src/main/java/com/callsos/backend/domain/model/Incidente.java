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
 *
 * Máquina de estados (ver EstadoIncidente para el diagrama completo):
 *   CREADO → DERIVADO_A_CAI → AGENTE_ASIGNADO → AGENTE_EN_CAMINO
 *   → EN_ATENCION → FINALIZADO
 *   Desde cualquier estado activo: → CANCELADO
 */
@Getter
public class Incidente {
 
    private final String id;
    private final LocalDateTime fechaHora;
    private final TipoIncidente tipo;
    private String descripcion;
    private EstadoIncidente estado;
    private final Ubicacion ubicacion;
    private final Denunciante denunciante;
    private UnidadPolicial unidadPolicial;
    private Denuncia denuncia;
    private final List<Asignacion> asignaciones;
 
    /** Constructor principal — crea un incidente nuevo. */
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
 
    // ── Operaciones de negocio ─────────────────────────────────────────────
 
    /**
     * El Comando deriva el incidente al CAI más cercano.
     * Solo válido en estado CREADO.
     */
    public void derivarACAI(UnidadPolicial cai) {
        validarTransicion(this.estado, EstadoIncidente.DERIVADO_A_CAI);
        this.unidadPolicial = cai;
        this.estado = EstadoIncidente.DERIVADO_A_CAI;
    }
 
    /**
     * El CAI asigna un agente. Solo válido en DERIVADO_A_CAI.
     * La transición a AGENTE_ASIGNADO la gestiona AsignarAgenteService
     * después de crear la Asignacion.
     */
    public void marcarAgenteAsignado() {
        validarTransicion(this.estado, EstadoIncidente.AGENTE_ASIGNADO);
        this.estado = EstadoIncidente.AGENTE_ASIGNADO;
    }
 
    /**
     * El agente acepta el incidente y sale hacia el lugar.
     * Activa el tracking en tiempo real (Fase 2).
     */
    public void marcarAgenteEnCamino() {
        validarTransicion(this.estado, EstadoIncidente.AGENTE_EN_CAMINO);
        this.estado = EstadoIncidente.AGENTE_EN_CAMINO;
    }
 
    /**
     * El agente llega y comienza la atención activa.
     */
    public void iniciarAtencion() {
        validarTransicion(this.estado, EstadoIncidente.EN_ATENCION);
        this.estado = EstadoIncidente.EN_ATENCION;
    }
 
    /**
     * El agente finaliza la atención.
     */
    public void finalizar() {
        validarTransicion(this.estado, EstadoIncidente.FINALIZADO);
        this.estado = EstadoIncidente.FINALIZADO;
    }
 
    /**
     * El denunciante cancela la solicitud.
     * Válido desde cualquier estado activo (no desde FINALIZADO ni CANCELADO).
     */
    public void cancelar() {
        if (EstadoIncidente.FINALIZADO.equals(this.estado) ||
            EstadoIncidente.CANCELADO.equals(this.estado))
            throw new IllegalStateException(
                "No se puede cancelar un incidente " + this.estado);
        this.estado = EstadoIncidente.CANCELADO;
    }
 
    /**
     * Cambio de estado genérico — mantiene compatibilidad con
     * los casos de uso existentes (CambiarEstadoIncidenteService).
     */
    public void cambiarEstado(EstadoIncidente nuevoEstado) {
        if (nuevoEstado == EstadoIncidente.CANCELADO) {
            cancelar();
            return;
        }
        validarTransicion(this.estado, nuevoEstado);
        this.estado = nuevoEstado;
    }
 
    /** Agrega una asignación de agente a este incidente. */
    public void agregarAsignacion(Asignacion asignacion) {
        if (asignacion == null)
            throw new IllegalArgumentException("La asignación no puede ser nula.");
        if (EstadoIncidente.FINALIZADO.equals(this.estado) ||
            EstadoIncidente.CANCELADO.equals(this.estado))
            throw new IllegalStateException(
                "No se pueden agregar asignaciones a un incidente " + this.estado);
        this.asignaciones.add(asignacion);
    }
 
    public Denuncia getDenuncia() { return denuncia; }
 
    public void setDenuncia(Denuncia denuncia) {
        if (this.denuncia != null)
            throw new IllegalStateException("El incidente ya tiene una Denuncia vinculada.");
        this.denuncia = denuncia;
    }
 
    public List<Asignacion> getAsignaciones() {
        return Collections.unmodifiableList(asignaciones);
    }
 
    public boolean estaActivo() {
        return !EstadoIncidente.FINALIZADO.equals(this.estado) &&
               !EstadoIncidente.CANCELADO.equals(this.estado);
    }
 
    // ── Reconstitución desde persistencia ────────────────────────────────────
 
    /**
     * Restaura el estado desde BD sin pasar por la máquina de transiciones.
     * SOLO para uso de adaptadores de persistencia al reconstruir el agregado.
     * Nunca llamar desde lógica de negocio.
     */
    public void reconstituirEstado(EstadoIncidente estado) {
        this.estado = estado;
    }
 
    /**
     * Restaura la unidad policial desde BD.
     * SOLO para uso de adaptadores de persistencia.
     */
    public void reconstituirUnidad(UnidadPolicial unidad) {
        this.unidadPolicial = unidad;
    }
 
    // ── Máquina de estados ─────────────────────────────────────────────────
 
    private void validarTransicion(EstadoIncidente actual, EstadoIncidente siguiente) {
        boolean valida = switch (actual) {
            case CREADO           -> siguiente == EstadoIncidente.DERIVADO_A_CAI;
            case DERIVADO_A_CAI   -> siguiente == EstadoIncidente.AGENTE_ASIGNADO;
            case AGENTE_ASIGNADO  -> siguiente == EstadoIncidente.AGENTE_EN_CAMINO;
            case AGENTE_EN_CAMINO -> siguiente == EstadoIncidente.EN_ATENCION;
            case EN_ATENCION      -> siguiente == EstadoIncidente.FINALIZADO;
            case FINALIZADO, CANCELADO -> false;
        };
        if (!valida)
            throw new IllegalStateException(
                "Transición inválida: " + actual + " → " + siguiente);
    }
}