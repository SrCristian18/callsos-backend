/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita @Async en toda la aplicación.
 *
 * Necesario para que NotificacionEventListener procese en background:
 * el hilo del request HTTP termina con 204 sin esperar a Firebase.
 *
 * Spring usa un ThreadPoolTaskExecutor por defecto.
 * En producción se puede configurar el pool size según la carga.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Configuración mínima: @EnableAsync activa el soporte asíncrono.
    // Para controlar el pool de hilos, se puede definir un @Bean Executor aquí.
}
