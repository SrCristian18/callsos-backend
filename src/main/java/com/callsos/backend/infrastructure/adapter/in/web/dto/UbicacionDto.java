/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

/**
 *
 * @author LENOVO
 */

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * DTO que representa una coordenada geográfica en el JSON.
 * Se convierte al value object Ubicacion mediante el mapper.
 */
public class UbicacionDto {
    
    @DecimalMin(value = "-90.0",  message = "Latitud mínima: -90")
    @DecimalMax(value = "90.0",   message = "Latitud máxima: 90")
    private double latitud;
 
    @DecimalMin(value = "-180.0", message = "Longitud mínima: -180")
    @DecimalMax(value = "180.0",  message = "Longitud máxima: 180")
    private double longitud;
 
    public double getLatitud()  { return latitud; }
    public double getLongitud() { return longitud; }
}
