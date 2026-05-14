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
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
/**
 * Centro de Atención Inmediata (CAI).
 * Hereda de AutoridadPolicial (diagrama 2) y agrega agentes
 * mediante composición (diagrama 2, diamante).
 * Corresponde a la clase "CAI" del diagrama de atributos.
 */
@Getter
public class UnidadPolicial extends AutoridadPolicial {
 
    private final List<Agente> agentes;
 
    public UnidadPolicial(String id, String nombre,
                          String direccion, Ubicacion ubicacion,
                          String telefono) {
        super(id, nombre, direccion, ubicacion, telefono);
        this.agentes = new ArrayList<>();
    }
 
    /** Agrega un agente a la unidad. */
    public void agregarAgente(Agente agente) {
        if (agente == null) throw new IllegalArgumentException("Agente no puede ser nulo");
        agentes.add(agente);
    }
 
    /** Vista no modificable de los agentes. */
    public List<Agente> getAgentes() {
        return Collections.unmodifiableList(agentes);
    }
}
