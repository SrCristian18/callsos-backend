/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.persistence;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Tests unitarios de GlobalExceptionHandler.
 *
 * Verifica que cada tipo de excepción produce el código HTTP correcto
 * y que el handler genérico NO expone el stack trace en el body.
 */
@DisplayName("GlobalExceptionHandler — códigos HTTP y seguridad")
public class GlobalExceptionHandlerTest {
    
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
 
    @Test
    @DisplayName("IllegalArgumentException → 404 Not Found")
    void illegalArgument_produce404() {
        ProblemDetail result = handler.handleNotFound(
            new IllegalArgumentException("Incidente no encontrado: abc-123"));
 
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Incidente no encontrado: abc-123", result.getDetail());
        assertEquals("Recurso no encontrado", result.getTitle());
    }
 
    @Test
    @DisplayName("IllegalStateException → 422 Unprocessable Entity")
    void illegalState_produce422() {
        ProblemDetail result = handler.handleBusinessRule(
            new IllegalStateException("Transición inválida: CREADO → FINALIZADO"));
 
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getStatus());
        assertEquals("Regla de negocio violada", result.getTitle());
    }
 
    @Test
    @DisplayName("Exception genérica → 500 sin exponer stack trace al cliente")
    void excepcionGenerica_produce500SinStackTrace() {
        ProblemDetail result = handler.handleGeneric(
            new RuntimeException("Error de base de datos: connection refused to 192.168.1.x"));
 
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
 
        // SEGURIDAD: el body nunca debe contener rutas internas, IPs, ni el mensaje real
        String detail = result.getDetail();
        assertNotNull(detail);
        assertFalse(detail.contains("192.168"),
            "El detail NO debe exponer IPs internas");
        assertFalse(detail.contains("connection refused"),
            "El detail NO debe exponer detalles técnicos del error");
        assertEquals("Error interno del servidor. Por favor intente más tarde.", detail);
    }
 
    @Test
    @DisplayName("Todos los handlers incluyen timestamp en la respuesta")
    void todosLosHandlersIncluyenTimestamp() {
        ProblemDetail p1 = handler.handleNotFound(new IllegalArgumentException("x"));
        ProblemDetail p2 = handler.handleBusinessRule(new IllegalStateException("x"));
        ProblemDetail p3 = handler.handleGeneric(new Exception("x"));
 
        assertNotNull(p1.getProperties().get("timestamp"));
        assertNotNull(p2.getProperties().get("timestamp"));
        assertNotNull(p3.getProperties().get("timestamp"));
    }
}
