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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Componente base del patrón Composite.
 *
 * Tanto Agente (hoja) como UnidadPolicial (nodo compuesto)
 * heredan de esta clase. AutoridadPolicial mantiene una lista
 * de sus componentes hijo, lo que permite construir jerarquías
 * arbitrarias: una UnidadPolicial puede contener Agentes u otras
 * UnidadesPolicial.
 *
 * La agregación (rombo blanco) del diagrama 2 se representa con
 * la lista `subordinados` — AutoridadPolicial agrega instancias
 * de sí misma.
 *
 * ══════════════════════════════════════════════════════════════════
 * ESTADO (Épica 8, hallazgo #8.1 — auditoría de código muerto):
 * ══════════════════════════════════════════════════════════════════
 * A la fecha de esta nota, {@link #agregar}, {@link #eliminar} y
 * {@link #getSubordinados} NUNCA se invocan desde ningún caso de uso
 * real — `subordinados` queda siempre vacía en producción. La
 * jerarquía de mando real hoy es plana (un CAI tiene Agentes vía
 * `AgenteRepositoryPort`, ver `UnidadPolicial.getAgentes()`, que
 * consulta BD directamente y NO pasa por este Composite).
 *
 * SE DEJA A PROPÓSITO (decisión explícita, no descuido): este
 * Composite queda preparado para una futura jerarquía real de
 * unidades (ej. una UnidadPolicial que reporte a otra UnidadPolicial
 * de rango superior — comisarías, zonas, etc.), que el modelo de
 * dominio actual no contempla todavía. Si se implementa esa
 * funcionalidad, este es el mecanismo a poblar (vía `agregar()` al
 * cargar la jerarquía desde BD) y consultar (vía `getSubordinados()`
 * o los helpers de `UnidadPolicial`).
 *
 * Antes de darle uso real, revisar:
 * - Quién puebla `subordinados` al reconstruir desde BD (hoy ningún
 *   RowMapper lo hace — la tabla `unidades_policiales` no tiene ni
 *   siquiera una columna de unidad superior).
 * - Si `getAgentes()`/`getAgentesDisponibles()` de `UnidadPolicial`
 *   deberían seguir yendo directo a BD (más simple, ya probado) o
 *   empezar a apoyarse en este Composite (más fiel al patrón, pero
 *   requiere mantener el árbol en memoria sincronizado con BD).
 * ══════════════════════════════════════════════════════════════════
 */

public abstract class AutoridadPolicial {
 
    protected final String id;       // UUID
    protected String nombre;
    protected String direccion;
    protected Ubicacion ubicacion;
    protected String telefono;

    /**
     * Épica 5 — token FCM para notificaciones push a Agente/UnidadPolicial
     * (CAI). Mismo propósito que Denunciante.tokenFcm, pero vive acá (no
     * en las subclases) porque tanto Agente como UnidadPolicial lo
     * necesitan y ambas heredan de esta clase — evita duplicar el campo.
     * Nullable/mutable (no en el constructor) porque el token se registra
     * DESPUÉS de que el agente/CAI ya existe en BD, igual que en
     * Denunciante — ver RegistrarTokenFcmAgenteService/RegistrarTokenFcmUnidadService.
     */
    protected String tokenFcm;

    /**
     * Épica 8 (hallazgo #6, Parte 1): correo de contacto — requisito para
     * poder implementar recuperación de contraseña por email en un paso
     * posterior de la misma épica. Mismo criterio que tokenFcm: vive acá
     * (no en las subclases) porque tanto Agente como UnidadPolicial lo
     * necesitan. A diferencia de tokenFcm, SÍ se recoge en el registro
     * (para Agente — ver RegistrarAgenteConInvitacionService); aun así se
     * deja mutable/nullable (no en el constructor) porque las cuentas YA
     * existentes en BD (seed) no lo tienen y no hay forma de backfillarlo
     * retroactivamente. UnidadPolicial no tiene flujo de registro en la
     * app (los CAI se crean solo por seed SQL) — la columna queda lista
     * para un futuro "Comando crea un CAI", pero nada la puebla todavía.
     */
    protected String correo;
    
    /** Lista de componentes hijo (Composite). */
    private final List<AutoridadPolicial> subordinados;
 
    protected AutoridadPolicial(String id, String nombre,
                                String direccion, Ubicacion ubicacion,
                                String telefono) {
        this.id        = id;
        this.nombre    = nombre;
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.telefono  = telefono;
        this.subordinados = new ArrayList<>();
    }
    
     // ── Operaciones Composite (Épica 8, hallazgo #8.1: sin uso real hoy
     // — preparado para una futura jerarquía de unidades, ver docstring
     // de la clase) ───────────────────────────────────────────────────
 
    /**
     * Agrega un subordinado (Agente u otra UnidadPolicial).
     * Las hojas (Agente) sobreescriben este método lanzando excepción
     * porque no pueden tener hijos.
     */
    public void agregar(AutoridadPolicial componente) {
        if (componente == null)
            throw new IllegalArgumentException("El componente no puede ser nulo.");
        subordinados.add(componente);
    }
    
    /**
     * Elimina un subordinado del nodo.
     */
    public void eliminar(AutoridadPolicial componente) {
        subordinados.remove(componente);
    }
    
    /** Vista inmutable de los subordinados. */
    public List<AutoridadPolicial> getSubordinados() {
        return Collections.unmodifiableList(subordinados);
    }
    
    // ── Getters ────────────────────────────────────────────────────────────
 
    public String getId()          { return id; }
    public String getNombre()      { return nombre; }
    public String getDireccion()   { return direccion; }
    public Ubicacion getUbicacion(){ return ubicacion; }
    public String getTelefono()    { return telefono; }
    public String getTokenFcm()    { return tokenFcm; }

    /**
     * Se setea después de construir el objeto (normalmente al mapear la
     * fila de BD) — igual razón que Agente.estado: el token no es parte
     * del "constructor de negocio" (crear un Agente/CAI no requiere un
     * token todavía), se adjunta cuando el dispositivo lo registra.
     */
    public void setTokenFcm(String tokenFcm) {
        this.tokenFcm = tokenFcm;
    }

    public boolean tieneTokenFcm() {
        return tokenFcm != null && !tokenFcm.isBlank();
    }

    /**
     * Se setea después de construir el objeto — misma razón que
     * {@link #setTokenFcm}. Para Agente, se llama inmediatamente después
     * de crear la instancia durante el registro (dato ya disponible en
     * ese momento, a diferencia del token FCM); al reconstituir desde
     * BD, puede ser null (cuentas sembradas antes de este fix).
     */
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCorreo() {
        return correo;
    }
 
    /**
     * Operación polimórfica del Composite.
     * Cada subclase describe su rol en la jerarquía.
     */
    public abstract String getRol();
}