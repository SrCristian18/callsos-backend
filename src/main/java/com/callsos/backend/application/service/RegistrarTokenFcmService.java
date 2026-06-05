/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.port.in.RegistrarTokenFcmPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
 
/**
 * Caso de uso: registrar o actualizar el token FCM de un denunciante.
 *
 * Orquesta:
 *   1. Verificar que el denunciante existe.
 *   2. Actualizar el token en BD.
 *
 * Sin este caso de uso activo, las notificaciones push de Firebase
 * llegarán a tokens desactualizados y serán silenciosas (FCM las descarta).
 */
public class RegistrarTokenFcmService implements RegistrarTokenFcmPort {
    
    private final DenuncianteRepositoryPort denuncianteRepository;
 
    public RegistrarTokenFcmService(DenuncianteRepositoryPort denuncianteRepository) {
        this.denuncianteRepository = denuncianteRepository;
    }
 
    @Override
    public void ejecutar(String denuncianteId, String tokenFcm) {
 
        // Verificar existencia — lanza 404 si no existe (GlobalExceptionHandler)
        denuncianteRepository.buscarPorId(denuncianteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Denunciante no encontrado: " + denuncianteId));
 
        denuncianteRepository.actualizarTokenFcm(denuncianteId, tokenFcm);
    }
}
