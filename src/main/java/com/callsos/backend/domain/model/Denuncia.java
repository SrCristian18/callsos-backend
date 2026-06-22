/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
 
import java.time.LocalDateTime;
import java.util.Objects;
 
/**
 * Clase ternaria: surge de la relación entre Denunciante e Incidente.
 *
 * Regla de negocio: no puede existir una Denuncia si no hay un
 * Denunciante y un Incidente. Ambas referencias son obligatorias
 * y se validan en el constructor.
 *
 * Es el origen de una Asignacion — sin Denuncia no hay Asignacion.
 */

public class Denuncia {
 
    private final String id;                  // UUID
    private final LocalDateTime fecha;
    private final TipoIncidente tipo;
    private final String descripcion;
    private final Ubicacion ubicacion;
    private final Denunciante denunciante;  // OBLIGATORIO
    private final Incidente incidente;        // OBLIGATORIO
   
    public Denuncia(String id, TipoIncidente tipo, String descripcion,
                    Ubicacion ubicacion,
                    Denunciante denunciante,
                    Incidente incidente) {
 
        Objects.requireNonNull(denunciante,
            "Una Denuncia requiere un Denunciante.");
        Objects.requireNonNull(incidente,
            "Una Denuncia requiere un Incidente.");
        Objects.requireNonNull(tipo,
            "El tipo de incidente es obligatorio.");
 
        this.id           = id;
        this.tipo         = tipo;
        this.descripcion  = descripcion;
        this.ubicacion    = ubicacion;
        this.denunciante  = denunciante;
        this.incidente    = incidente;
        this.fecha        = LocalDateTime.now();
    }

    // ── Constructor de RECONSTITUCIÓN — sin efectos de dominio ────────────
    //
    // FIX (validación end-to-end): mismo patrón ya usado por Asignacion
    // (ver Asignacion.reconstituir) — necesario porque Denuncia <-> Incidente
    // tienen una referencia mutua: el Incidente reconstruido desde BD
    // (IncidenteRepositoryMySQL.mapRow) debe poder pasarse aquí como
    // parámetro YA CONSTRUIDO, evitando el ciclo infinito de
    // "para construir Denuncia necesito Incidente, para construir
    // Incidente necesito Denuncia".
    //
    // No valida disponibilidad de nada ni dispara efectos de dominio —
    // el estado ya existe en BD, solo se restaura en memoria.
    private Denuncia(String id, LocalDateTime fecha, TipoIncidente tipo,
                     String descripcion, Ubicacion ubicacion,
                     Denunciante denunciante, Incidente incidente) {
        this.id           = id;
        this.fecha         = fecha;
        this.tipo         = tipo;
        this.descripcion  = descripcion;
        this.ubicacion    = ubicacion;
        this.denunciante  = denunciante;
        this.incidente    = incidente;
    }

    /**
     * Factory method de reconstitución — punto de entrada para los
     * adaptadores de persistencia.
     *
     * Uso: Denuncia.reconstituir(id, fecha, tipo, descripcion, ubicacion,
     *                             denunciante, incidente)
     */
    public static Denuncia reconstituir(String id, LocalDateTime fecha,
                                        TipoIncidente tipo, String descripcion,
                                        Ubicacion ubicacion,
                                        Denunciante denunciante,
                                        Incidente incidente) {
        Objects.requireNonNull(id, "ID de denuncia requerido para reconstitución.");
        Objects.requireNonNull(denunciante, "Denunciante requerido para reconstitución.");
        Objects.requireNonNull(incidente, "Incidente requerido para reconstitución.");
        return new Denuncia(id, fecha, tipo, descripcion, ubicacion, denunciante, incidente);
    }
    
    public String getId()               { return id; }
    public LocalDateTime getFecha()     { return fecha; }
    public TipoIncidente getTipo()      { return tipo; }
    public String getDescripcion()      { return descripcion; }
    public Ubicacion getUbicacion()     { return ubicacion; }
    public Denunciante getDenunciante() { return denunciante; }
    public Incidente getIncidente()     { return incidente; }
}