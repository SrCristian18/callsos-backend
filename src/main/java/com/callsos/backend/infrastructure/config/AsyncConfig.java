/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Habilita @Async en toda la aplicación.
 *
 * Necesario para que NotificacionEventListener y AuditoriaEventListener
 * procesen en background: el hilo del request HTTP no espera a Firebase
 * ni a la escritura de auditoría.
 *
 * PROPAGACIÓN DEL SecurityContext (importante):
 * Por defecto, Spring Security usa SecurityContextHolder en modo
 * MODE_THREADLOCAL, que NO se propaga a los hilos nuevos que crea
 * @Async. Sin la configuración de abajo, AuditoriaEventListener
 * (que lee SecurityContextHolder.getContext().getAuthentication()
 * para saber quién hizo el cambio) siempre habría recibido un
 * contexto vacío y habría registrado actorId="sistema" para TODAS
 * las auditorías, sin importar el usuario real.
 *
 * La solución es envolver el Executor con DelegatingSecurityContextExecutor
 * (provisto por spring-security-core), que copia el SecurityContext del
 * hilo que dispara el evento hacia el hilo @Async que lo procesa. Esta
 * solución es independiente del tipo de Executor (a diferencia de usar
 * SecurityContextHolder.MODE_INHERITABLETHREADLOCAL, que deja de
 * funcionar de forma confiable si en el futuro se usa un
 * ThreadPoolTaskExecutor con hilos reutilizados).
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("callsos-async-");
        executor.initialize();

        // Envuelve el executor para que cada tarea @Async herede el
        // SecurityContext del hilo que la disparó.
        return new DelegatingSecurityContextExecutor(executor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // Sin esto, una excepción no capturada dentro de un método @Async
        // se pierde silenciosamente — no llega al hilo del request
        // (que ya respondió) ni aparece en ningún log por defecto.
        return (Throwable ex, Method method, Object... params) ->
            log.error("Excepción no capturada en método @Async '{}': {}",
                method.getName(), ex.getMessage(), ex);
    }
}