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
 * Reporte generado a nivel de AutoridadPolicial
 * para análisis y seguimiento estadístico (diagrama 2).
 */
@Getter
@AllArgsConstructor
public class ReporteAdministrativo {
 
    private final String id;                      // UUID
    private final LocalDateTime fecha;
    private final String resumen;
    private final AutoridadPolicial autoridad;
}
