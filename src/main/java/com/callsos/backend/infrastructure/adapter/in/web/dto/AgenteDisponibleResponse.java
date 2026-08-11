package com.callsos.backend.infrastructure.adapter.in.web.dto;

import com.callsos.backend.domain.enums.EstadoAgente;

/**
 * DTO de salida para Agente, usado por el listado de disponibles por CAI.
 *
 * NOTA: deuda_backend.md (Gap 3) proponía un campo "placa" en la
 * respuesta, pero el modelo de dominio Agente/AutoridadPolicial no tiene
 * ese campo hoy (solo id, nombre, direccion, telefono, ubicacion, estado).
 * Se expone "telefono" en su lugar por ser el dato de contacto real
 * disponible. Si el frontend necesita un identificador tipo "placa",
 * habría que agregarlo primero al modelo de dominio y a la tabla
 * "agentes" — no se inventa aquí un campo que no existe en el dominio.
 */
public class AgenteDisponibleResponse {

    private final String id;
    private final String nombre;
    private final String telefono;
    private final EstadoAgente estado;

    public AgenteDisponibleResponse(String id, String nombre,
                                     String telefono, EstadoAgente estado) {
        this.id       = id;
        this.nombre   = nombre;
        this.telefono = telefono;
        this.estado   = estado;
    }

    public String getId()             { return id; }
    public String getNombre()         { return nombre; }
    public String getTelefono()       { return telefono; }
    public EstadoAgente getEstado()   { return estado; }
}