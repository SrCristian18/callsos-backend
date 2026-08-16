package com.callsos.backend.domain.exception;

/**
 * Excepción de dominio: el actor autenticado no es el propietario/no está
 * autorizado a operar sobre el recurso solicitado.
 *
 * Distinta de IllegalStateException (regla de negocio sobre el propio
 * agregado, ej. transición de estado inválida) e IllegalArgumentException
 * (recurso no encontrado) — esta representa específicamente una violación
 * de OWNERSHIP/autorización y se mapea a 403 Forbidden en
 * GlobalExceptionHandler, nunca a 404 ni 422.
 *
 * Épica 1: usada por ActualizarTipoIncidenteService cuando el denunciante
 * autenticado no es el dueño del incidente que intenta modificar.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String message) {
        super(message);
    }
}
