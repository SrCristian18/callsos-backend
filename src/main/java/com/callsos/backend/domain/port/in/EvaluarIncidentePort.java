/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.in;

/**
 *
 * @author LENOVO
 */
import com.callsos.backend.domain.model.Incidente;
/**
 * Puerto de entrada: contrato para evaluar un incidente.
 * La evaluación puede incluir priorización, validación de datos
 * o generación de un ReporteHallazgos.
 */
public interface EvaluarIncidentePort {
    
    /**
     * @param incidente  Incidente a evaluar (ya cargado)
     */
    void ejecutar(Incidente incidente);
}
