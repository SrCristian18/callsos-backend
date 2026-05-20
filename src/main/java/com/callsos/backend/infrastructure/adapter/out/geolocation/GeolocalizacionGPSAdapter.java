/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.geolocation;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.out.GeolocalizacionPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.springframework.stereotype.Component;
 
/**
 * Adaptador de salida: implementa GeolocalizacionPuerto usando un servicio GPS externo.
 *
 * Actualmente es un stub. La integración real depende del proveedor elegido:
 *   - Google Maps Geocoding API
 *   - OpenStreetMap / Nominatim (gratuito)
 *   - GPS del dispositivo móvil vía WebSocket
 *
 * Para integrarlo con Google Maps, agregar al pom.xml:
 *   <dependency>
 *     <groupId>com.google.maps</groupId>
 *     <artifactId>google-maps-services</artifactId>
 *     <version>2.2.0</version>
 *   </dependency>
 *
 * Y configurar en application.yml:
 *   google.maps.api-key: ${GOOGLE_MAPS_KEY}
 */
@Component
public class GeolocalizacionGPSAdapter implements GeolocalizacionPort{
    
    /*
     * Cuando se integre Google Maps:
     *
     * @Value("${google.maps.api-key}")
     * private String apiKey;
     *
     * private GeoApiContext buildContext() {
     *     return new GeoApiContext.Builder().apiKey(apiKey).build();
     * }
     */
 
    @Override
    public Ubicacion obtenerUbicacionActual() {
        /*
         * Implementación real: llamada al GPS del dispositivo o
         * al servicio de geolocalización por IP del servidor.
         * Por ahora retorna coordenadas de Cartagena, Colombia
         * como valor por defecto para pruebas.
         */
        return new Ubicacion(10.3910, -75.4794);
    }
 
    @Override
    public boolean validarUbicacion(Ubicacion ubicacion) {
        /*
         * Implementación real: verificar contra la API de Google Maps
         * que las coordenadas corresponden a una dirección válida
         * dentro del área de cobertura del sistema.
         *
         * GeocodingResult[] results = GeocodingApi
         *     .reverseGeocode(buildContext(), new LatLng(ubicacion.getLatitud(), ubicacion.getLongitud()))
         *     .await();
         * return results != null && results.length > 0;
         */
 
        // Stub: la validación de rango ya ocurre en el value object Ubicacion
        return ubicacion != null;
    }
}
