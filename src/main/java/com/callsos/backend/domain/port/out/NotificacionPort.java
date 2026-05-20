/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.model.Denunciante;
 
/**
 * Puerto de salida: contrato para el servicio de notificaciones.
 *
 * El dominio solo sabe que puede notificar a un Denunciante con un mensaje.
 * La implementación concreta (NotificacionFirebaseAdapter) decide
 * cómo se entrega la notificación (push, SMS, email, etc.).
 */
public interface NotificacionPort {
    
    /**
     * Envía una notificación al denunciante con el mensaje dado.
     *
     * @param denunciante  Destinatario de la notificación
     * @param mensaje      Contenido del mensaje
     */
    void notificarDenunciante(Denunciante denunciante, String mensaje);
}
