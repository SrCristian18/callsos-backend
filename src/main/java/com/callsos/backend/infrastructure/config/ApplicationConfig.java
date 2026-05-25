/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.application.service.AsignarAgenteService;
import com.callsos.backend.application.service.AsignarCAIAIncidenteService;
import com.callsos.backend.application.service.AtenderIncidenteService;
import com.callsos.backend.application.service.CambiarEstadoIncidenteService;
import com.callsos.backend.application.service.ConsultarEstadoIncidenteService;
import com.callsos.backend.application.service.CrearIncidenteService;
import com.callsos.backend.application.service.CrearReporteAdministrativoService;
import com.callsos.backend.application.service.CrearReporteHallazgosService;
import com.callsos.backend.application.service.EvaluarIncidenteService;
import com.callsos.backend.domain.port.in.AsignarAgentePort;
import com.callsos.backend.domain.port.in.AsignarCAIAIncidentePort;
import com.callsos.backend.domain.port.in.AtenderIncidentePort;
import com.callsos.backend.domain.port.in.CambiarEstadoIncidentePort;
import com.callsos.backend.domain.port.in.ConsultarEstadoIncidentePort;
import com.callsos.backend.domain.port.in.CrearIncidentePort;
import com.callsos.backend.domain.port.in.CrearReporteAdministrativoPort;
import com.callsos.backend.domain.port.in.CrearReporteHallazgosPort;
import com.callsos.backend.domain.port.in.EvaluarIncidentePort;
import com.callsos.backend.domain.port.out.AgenteByIdRepositoryPort;
import com.callsos.backend.domain.port.out.AgenteRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteAdministrativoRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de la capa de aplicación.
 *
 * Responsabilidad: registrar cada caso de uso como bean de Spring,
 * inyectándole explícitamente los puertos de salida que necesita.
 *
 * POR QUÉ @Bean y no @Service en los casos de uso:
 *   - Los casos de uso viven en la capa de aplicación, que no debe conocer
 *     anotaciones de Spring (principio de independencia de framework).
 *   - Esta clase es el único punto donde Spring "entra" en la capa de aplicación.
 *   - Permite ver de un vistazo qué implementación resuelve cada puerto.
 *   - Facilita cambiar implementaciones (ej: mock en tests) sin tocar los servicios.
 *
 * POR QUÉ el tipo de retorno es la INTERFAZ (Port) y no la clase (Service):
 *   - Spring registra el bean bajo el tipo declarado.
 *   - El IncidenteController inyecta por tipo de puerto → Spring lo resuelve aquí.
 *   - Si el tipo fuera la clase concreta, el controller tendría que conocerla,
 *     rompiendo el aislamiento hexagonal.
 */
@Configuration
public class ApplicationConfig {
    
    /**
     * Caso de uso: crear un incidente.
     * Necesita: saber quién denuncia (DenuncianteRepo) y dónde persistir (IncidenteRepo).
     */
    @Bean
    public CrearIncidentePort crearIncidentePort(
            IncidenteRepositoryPort incidenteRepository,
            DenuncianteRepositoryPort denuncianteRepository) {
        return new CrearIncidenteService(incidenteRepository, denuncianteRepository);
    }
    
    @Bean
    public AsignarCAIAIncidentePort asignarCAIAIncidentePort(
            IncidenteRepositoryPort incidenteRepo,
            UnidadPolicialRepositoryPort unidadRepo) {
        return new AsignarCAIAIncidenteService(incidenteRepo, unidadRepo);
    }
    
    /**
     * Caso de uso: cambiar el estado de un incidente manualmente.
     */
    @Bean
    public CambiarEstadoIncidentePort cambiarEstadoIncidentePort(
            IncidenteRepositoryPort incidenteRepository) {
        return new CambiarEstadoIncidenteService(incidenteRepository);
    }
 
    /**
     * Caso de uso: consultar el estado actual (solo lectura).
     */
    @Bean
    public ConsultarEstadoIncidentePort consultarEstadoIncidentePort(
            IncidenteRepositoryPort incidenteRepository) {
        return new ConsultarEstadoIncidenteService(incidenteRepository);
    }
 
    /**
     * Caso de uso: asignar un agente disponible al incidente.
     * Necesita: buscar agentes (AgenteRepo) y cargar/guardar el incidente (IncidenteRepo).
     */
    @Bean
    public AsignarAgentePort asignarAgentePort(
            AgenteRepositoryPort agenteRepository,
            IncidenteRepositoryPort incidenteRepository) {
        return new AsignarAgenteService(agenteRepository, incidenteRepository);
    }
 
    /**
     * Caso de uso: marcar el incidente como EN_PROCESO
     * (el agente llegó al lugar del hecho).
     */
    @Bean
    public AtenderIncidentePort atenderIncidentePort(
            IncidenteRepositoryPort incidenteRepository) {
        return new AtenderIncidenteService(incidenteRepository);
    }
 
    /**
     * Caso de uso: evaluar y finalizar un incidente atendido.
     */
    @Bean
    public EvaluarIncidentePort evaluarIncidentePort(
            IncidenteRepositoryPort incidenteRepository) {
        return new EvaluarIncidenteService(incidenteRepository);
    }
    
    @Bean
    public CrearReporteHallazgosPort crearReporteHallazgosPort(
            IncidenteRepositoryPort incidenteRepo,
            AgenteByIdRepositoryPort agenteRepo,
            ReporteHallazgosRepositoryPort reporteRepo) {
        return new CrearReporteHallazgosService(incidenteRepo, agenteRepo, reporteRepo);
    }
 
    @Bean
    public CrearReporteAdministrativoPort crearReporteAdministrativoPort(
            IncidenteRepositoryPort incidenteRepo,
            UnidadPolicialRepositoryPort unidadRepo,
            ReporteAdministrativoRepositoryPort reporteRepo) {
        return new CrearReporteAdministrativoService(incidenteRepo, unidadRepo, reporteRepo);
    }
}
