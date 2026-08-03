package com.callsos.backend.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.callsos.backend.domain.port.out.NotificacionPort;
import com.callsos.backend.infrastructure.adapter.out.notification.NotificacionFirebaseAdapter;
import com.callsos.backend.infrastructure.adapter.out.notification.NotificacionNoOpAdapter;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
public class NotificationConfig {
    
    @Bean
    @ConditionalOnBean(FirebaseMessaging.class)
    public NotificacionPort firebaseNotificacion(
        FirebaseMessaging firebaseMessaging){
            return new NotificacionFirebaseAdapter(firebaseMessaging);
        }


    @Bean
    @ConditionalOnMissingBean(NotificacionPort.class)
    public NotificacionPort noOpNotification()
    {
        return new NotificacionNoOpAdapter();
    }

}
