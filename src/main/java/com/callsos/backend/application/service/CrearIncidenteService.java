/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

/**
 *
 * @author LENOVO
 */
 
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.port.in.CrearIncidentePort;
import com.callsos.backend.domain.port.out.DenunciaRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.EventPublisherPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
 
import java.util.UUID;
 
/**
 * Caso de uso: crear un nuevo incidente a partir de una denuncia.
 *
 * Implementa CrearIncidentePort (puerto de entrada).
 * Depende de IncidenteRepositoryPort, DenuncianteRepositoryPort y
 * DenunciaRepositoryPort (puertos de salida) — nunca de clases de
 * infraestructura directamente.
 *
 * FIX (validación end-to-end): antes este servicio creaba el Incidente
 * pero NUNCA creaba la Denuncia asociada (la clase Denuncia.java define
 * en su propio comentario: "Es el origen de una Asignacion — sin
 * Denuncia no hay Asignacion"). Como resultado, AsignarAgenteService
 * rechazaba con IllegalStateException("El incidente no tiene una
 * Denuncia vinculada.") CUALQUIER incidente creado por el flujo real de
 * la aplicación — el bug bloqueaba el ciclo de vida completo de un
 * incidente más allá de DERIVADO_A_CAI, sin importar los datos.
 */
public class CrearIncidenteService implements CrearIncidentePort {
    
    private final IncidenteRepositoryPort incidenteRepository;
    private final DenuncianteRepositoryPort denuncianteRepository;
    private final DenunciaRepositoryPort denunciaRepository;
    private final EventPublisherPort eventPublisher;
 
    public CrearIncidenteService(IncidenteRepositoryPort incidenteRepository,
                                 DenuncianteRepositoryPort denuncianteRepository,
                                 DenunciaRepositoryPort denunciaRepository,
                                 EventPublisherPort eventPublisher) {
        this.incidenteRepository  = incidenteRepository;
        this.denuncianteRepository = denuncianteRepository;
        this.denunciaRepository   = denunciaRepository;
        this.eventPublisher       = eventPublisher;
    }
    
    @Override
    public Incidente ejecutar(String denuncianteId, TipoIncidente tipo,
                              String descripcion, Ubicacion ubicacion) {
 
        Denunciante denunciante = denuncianteRepository
            .buscarPorId(denuncianteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Denunciante no encontrado: " + denuncianteId));
 
        Incidente incidente = new Incidente(
            UUID.randomUUID().toString(),
            tipo,
            descripcion,
            ubicacion,
            denunciante
        );
 
        // guardar() es void — persistimos y retornamos el mismo objeto
        incidenteRepository.guardar(incidente);

        // FIX: crear y persistir la Denuncia asociada (orden obligatorio:
        // el Incidente debe existir en BD primero — ver comentario de
        // DenunciaRepositoryPort sobre FK violations).
        Denuncia denuncia = new Denuncia(
            UUID.randomUUID().toString(),
            tipo,
            descripcion,
            ubicacion,
            denunciante,
            incidente
        );
        denunciaRepository.guardar(denuncia);
        incidente.setDenuncia(denuncia);

        // Épica 2 (fix P4): antes CrearIncidenteService no publicaba
        // ningún evento — el CREADO nunca quedaba auditado.
        eventPublisher.publicar(new IncidenteEvent(
            incidente.getId(), denunciante.getId(), null, EstadoIncidente.CREADO));

        return incidente;
    }
}