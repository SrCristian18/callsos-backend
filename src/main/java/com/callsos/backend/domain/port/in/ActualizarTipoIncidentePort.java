package com.callsos.backend.domain.port.in;

import com.callsos.backend.domain.enums.TipoIncidente;

/**
 * Puerto de entrada: el DENUNCIANTE actualiza el tipo de su incidente
 * mientras la situación evoluciona (Épica 1).
 *
 * El actorId se recibe explícitamente (no se infiere del contexto de
 * seguridad dentro del caso de uso) para mantener la aplicación
 * testeable sin necesidad de un SecurityContext real — mismo patrón que
 * el resto de los puertos de entrada del proyecto, que reciben el actorId
 * ya extraído del JWT por el adaptador de entrada REST.
 */
public interface ActualizarTipoIncidentePort {

    /**
     * @param incidenteId ID del incidente a modificar.
     * @param actorId     ID del denunciante autenticado (extraído del JWT).
     * @param nuevoTipo   Tipo de incidente destino.
     */
    void ejecutar(String incidenteId, String actorId, TipoIncidente nuevoTipo);
}
