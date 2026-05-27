/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.callsos.backend.domain.port.out;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.event.IncidenteEvent;

/**
 * Puerto de salida: contrato para publicar eventos de dominio.
 *
 * Los casos de uso dependen de esta interfaz, no de Spring directamente.
 * La implementación usa ApplicationEventPublisher de Spring,
 * pero el dominio nunca importa Spring.
 *
 * Esto mantiene los casos de uso testables en aislamiento:
 * en tests se inyecta un mock de EventPublisherPort,
 * en producción se inyecta SpringEventPublisherAdapter.
 */
public interface EventPublisherPort {
    
    void publicar(IncidenteEvent evento);
}
