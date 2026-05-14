/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
import lombok.AllArgsConstructor;
import lombok.Getter;
 
import java.time.LocalDateTime;
 
/**
 * Reporte generado por un Agente al concluir la atención de un incidente.
 * Documenta los hallazgos en campo (diagrama 2).
 */
@Getter
@AllArgsConstructor
public class ReporteHallazgos {
 
    private final String id;                // UUID
    private final LocalDateTime fecha;
    private final String descripcion;
    private final Incidente incidente;
    private final Agente agente;
}
