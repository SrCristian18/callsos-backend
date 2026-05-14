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
import lombok.AllArgsConstructor;
import lombok.Getter;
 
import java.time.LocalDateTime;
 
/**
 * Reporte formal presentado por un Denunciante sobre un incidente.
 * Se vincula al Incidente resultante (diagrama 2).
 */
@Getter
@AllArgsConstructor
public class Denuncia {
 
    private final String id;                  // UUID
    private final LocalDateTime fecha;
    private final TipoIncidente tipo;
    private final String descripcion;
    private final Ubicacion ubicacion;
    private final Denunciante denunciante;
}
