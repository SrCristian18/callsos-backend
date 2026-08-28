/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.valueobject.Ubicacion;
 
import java.util.Optional;
 
/**
 * Puerto de salida: contrato de persistencia para UnidadPolicial (CAI).
 * Permite buscar la unidad más cercana a la ubicación de un incidente.
 */
public interface UnidadPolicialRepositoryPort {
    
    /** Busca el CAI más cercano a una ubicación (Haversine en SQL). */
    Optional<UnidadPolicial> buscarPorUbicacion(Ubicacion ubicacion);
 
    /** Busca una unidad por su ID — necesario para ReporteAdministrativo. */
    Optional<UnidadPolicial> buscarPorId(String id);

    /** Épica 5 — registra/actualiza el token FCM del CAI. */
    void actualizarTokenFcm(String unidadPolicialId, String tokenFcm);
}