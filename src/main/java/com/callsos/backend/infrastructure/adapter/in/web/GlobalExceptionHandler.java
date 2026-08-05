/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web;

/**
 *
 * @author LENOVO
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
 
import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;
 
/**
 * Manejador global de excepciones — RFC 7807 ProblemDetail.
 *
 * Sin esta clase, Spring devuelve 500 para cualquier excepción de dominio,
 * incluidas IllegalArgumentException e IllegalStateException que son errores
 * de cliente (400/422), no de servidor.
 *
 * ProblemDetail es el estándar RFC 7807 soportado nativamente desde Spring 6.
 * El cliente Flutter recibe un JSON estandarizado con:
 *   {
 *     "type":     "about:blank",
 *     "title":    "Bad Request",
 *     "status":   400,
 *     "detail":   "Incidente no encontrado: abc-123",
 *     "instance": "/api/incidentes/abc-123",
 *     "timestamp": "2025-01-01T00:00:00Z"
 *   }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
     /**
     * Recurso no encontrado → 404 Not Found.
     * Lanzado cuando buscarPorId() retorna Optional.empty() y el service hace orElseThrow.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Recurso no encontrado");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
 
    /**
     * Transición de estado inválida o regla de negocio violada → 422 Unprocessable Entity.
     * Lanzado por la máquina de estados de Incidente, por Asignacion, etc.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleBusinessRule(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Regla de negocio violada");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
 
    /**
     * Validación de DTO fallida (@Valid) → 400 Bad Request.
     * Incluye todos los campos con error para que el cliente Flutter pueda mostrarlos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
 
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Error de validación");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
 
    /**
     * Cualquier excepción no manejada → 500 Internal Server Error.
     * Nunca expone el stack trace al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {

        log.error("Error interno no manejado: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error interno del servidor. Por favor intente más tarde.");
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Error interno");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}