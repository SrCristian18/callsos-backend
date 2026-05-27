/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.out.event;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
 
/**
 * Adaptador de salida: implementa EventPublisherPort usando Spring.
 *
 * Es el único lugar del proyecto donde los casos de uso "tocan" Spring
 * de forma indirecta. El dominio solo conoce EventPublisherPort.
 *
 * ApplicationEventPublisher es síncrono por defecto en Spring.
 * Para hacerlo asíncrono (no bloquear el hilo del request mientras
 * Firebase envía la notificación), los @EventListener pueden anotarse
 * con @Async — configurado en AsyncConfig.
 */
@Component
public class SpringEventPublisherAdapter implements EventPublisherPort{
    
     private final ApplicationEventPublisher publisher;
 
    public SpringEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }
 
    @Override
    public void publicar(IncidenteEvent evento) {
        publisher.publishEvent(evento);
    }
}
