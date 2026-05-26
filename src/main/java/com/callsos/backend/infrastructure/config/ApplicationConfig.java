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
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteAdministrativoRepositoryPort;
import com.callsos.backend.domain.port.out.ReporteHallazgosRepositoryPort;
import com.callsos.backend.domain.port.out.UnidadPolicialRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
/**
 * Registro explícito de todos los casos de uso como beans de Spring.
 *
 * Cada @Bean conecta un Puerto de entrada con su implementación concreta,
 * inyectándole los Puertos de salida que necesita.
 *
 * Los adaptadores de salida (@Component) y los de servicios externos
 * (GeolocalizacionGPSAdapter, NotificacionFirebaseAdapter) son detectados
 * automáticamente por Spring via @Component — no necesitan @Bean aquí.
 * Solo los casos de uso (capa de aplicación, sin anotaciones Spring) necesitan
 * registro explícito.
 */
@Configuration
public class ApplicationConfig {
 
    // ── Incidente ──────────────────────────────────────────────────────────
 
    @Bean
    public CrearIncidentePort crearIncidentePort(
            IncidenteRepositoryPort incidenteRepo,
            DenuncianteRepositoryPort denuncianteRepo) {
        return new CrearIncidenteService(incidenteRepo, denuncianteRepo);
    }
 
    @Bean
    public AsignarCAIAIncidentePort asignarCAIAIncidentePort(
            IncidenteRepositoryPort incidenteRepo,
            UnidadPolicialRepositoryPort unidadRepo) {
        return new AsignarCAIAIncidenteService(incidenteRepo, unidadRepo);
    }
 
    /**
     * FIX: AsignarAgenteService ahora recibe AsignacionRepositoryPort
     * para persistir la Asignacion en BD (antes se creaba en memoria y se perdía).
     */
    @Bean
    public AsignarAgentePort asignarAgentePort(
            AgenteRepositoryPort agenteRepo,
            IncidenteRepositoryPort incidenteRepo,
            AsignacionRepositoryPort asignacionRepo) {
        return new AsignarAgenteService(agenteRepo, incidenteRepo, asignacionRepo);
    }
 
    @Bean
    public CambiarEstadoIncidentePort cambiarEstadoIncidentePort(
            IncidenteRepositoryPort incidenteRepo) {
        return new CambiarEstadoIncidenteService(incidenteRepo);
    }
 
    @Bean
    public ConsultarEstadoIncidentePort consultarEstadoIncidentePort(
            IncidenteRepositoryPort incidenteRepo) {
        return new ConsultarEstadoIncidenteService(incidenteRepo);
    }
 
    @Bean
    public AtenderIncidentePort atenderIncidentePort(
            IncidenteRepositoryPort incidenteRepo) {
        return new AtenderIncidenteService(incidenteRepo);
    }
 
    @Bean
    public EvaluarIncidentePort evaluarIncidentePort(
            IncidenteRepositoryPort incidenteRepo) {
        return new EvaluarIncidenteService(incidenteRepo);
    }
 
    // ── Reportes ───────────────────────────────────────────────────────────
 
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
    
    /**
     * Caso de uso: agente confirma que va en camino.
     * Puente entre flujo de negocio (Fase 1) y tracking WebSocket (Fase 2).
     */
    @Bean
    public com.callsos.backend.domain.port.in.MarcarAgenteEnCaminoPort marcarAgenteEnCaminoPort(
            IncidenteRepositoryPort incidenteRepo) {
        return new com.callsos.backend.application.service.MarcarAgenteEnCaminoService(incidenteRepo);
    }
}
 
    
