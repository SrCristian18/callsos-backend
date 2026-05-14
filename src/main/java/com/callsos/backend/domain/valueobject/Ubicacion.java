/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.valueobject;

import java.util.Objects;

public class Ubicacion {

    private final double latitud;
    private final double longitud;

    public Ubicacion(double latitud, double longitud) {
        if (latitud < -90 || latitud > 90)
            throw new IllegalArgumentException("Latitud inválida: " + latitud);
        if (longitud < -180 || longitud > 180)
            throw new IllegalArgumentException("Longitud inválida: " + longitud);
        this.latitud  = latitud;
        this.longitud = longitud;
    }
 
    public double getLatitud()  { return latitud; }
    public double getLongitud() { return longitud; }
    
    /**
     * Los value objects son inmutables.
     * "Actualizar" la ubicación significa crear una nueva instancia.
     */
    public Ubicacion actualizarUbicacion(Ubicacion nueva) {
        return new Ubicacion(nueva.latitud, nueva.longitud);
    }
    
    // igualdad por valor (IMPORTANTE en Value Objects)
     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ubicacion u)) return false;
        return Double.compare(u.latitud, latitud) == 0
            && Double.compare(u.longitud, longitud) == 0;
    }
 
    @Override
    public int hashCode() { return Objects.hash(latitud, longitud); }
 
    @Override
    public String toString() {
        return "Ubicacion{lat=" + latitud + ", lon=" + longitud + "}";
    }
}
