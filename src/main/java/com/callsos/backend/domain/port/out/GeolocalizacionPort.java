/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.valueobject.Ubicacion;
 
/**
 * Puerto de salida: contrato para el servicio de geolocalización.
 *
 * Define las operaciones que el dominio necesita del GPS/maps externo.
 * La implementación concreta (GeolocalizacionGPSAdapter) es la única
 * que conoce el proveedor real (Google Maps, OpenStreetMap, etc.).
 */
public interface GeolocalizacionPort {
    
    /**
     * Obtiene la ubicación actual del dispositivo o del agente.
     */
    Ubicacion obtenerUbicacionActual();
 
    /**
     * Valida si una ubicación dada corresponde a coordenadas reales
     * y accesibles dentro del área de operación.
     */
    boolean validarUbicacion(Ubicacion ubicacion);
}
