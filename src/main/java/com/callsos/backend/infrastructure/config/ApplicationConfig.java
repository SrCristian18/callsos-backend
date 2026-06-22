/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.config;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.application.service.*;
import com.callsos.backend.domain.port.in.*;
import com.callsos.backend.domain.port.out.*;
import com.callsos.backend.infrastructure.config.security.JwtService;
import com.callsos.backend.domain.port.in.RegistrarTokenFcmPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
 
/**
 * Registro explícito de todos los casos de uso como beans de Spring.
 *
 * Cada @Bean conecta un Puerto de entrada con su implementación concreta,
 * inyectándole los Puertos de salida que necesita.
 *
 * Los adaptadores (@Component) son detectados automáticamente.
 * Solo los casos de uso (sin anotaciones Spring) necesitan registro aquí.
 */
@Configuration
public class ApplicationConfig {
 
    // ── Seguridad / Autenticación ──────────────────────────────────────────
 
    /**
     * BCryptPasswordEncoder: verifica contraseñas hasheadas en tabla usuarios.
     * Se declara como @Bean para que Spring lo inyecte en LoginService
     * y también esté disponible para hashear contraseñas en otros puntos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    /**
     * Caso de uso: autenticar usuario con username + password → JWT.
     * Usa PasswordEncoder para verificar el hash BCrypt almacenado en BD.
     */
    @Bean
    public LoginPort loginPort(
            UsuarioRepositoryPort usuarioRepo,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        return new LoginService(usuarioRepo, jwtService, passwordEncoder);
    }
 
    // ── Incidente ──────────────────────────────────────────────────────────
 
    @Bean
    public CrearIncidentePort crearIncidentePort(
            IncidenteRepositoryPort incidenteRepo,
            DenuncianteRepositoryPort denuncianteRepo,
            DenunciaRepositoryPort denunciaRepo) {
        return new CrearIncidenteService(incidenteRepo, denuncianteRepo, denunciaRepo);
    }
 
    @Bean
    public AsignarCAIAIncidentePort asignarCAIAIncidentePort(
            IncidenteRepositoryPort incidenteRepo,
            UnidadPolicialRepositoryPort unidadRepo) {
        return new AsignarCAIAIncidenteService(incidenteRepo, unidadRepo);
    }
 
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
            IncidenteRepositoryPort incidenteRepo,
            EventPublisherPort eventPublisher) {
        return new EvaluarIncidenteService(incidenteRepo, eventPublisher);
    }
 
    @Bean
    public MarcarAgenteEnCaminoPort marcarAgenteEnCaminoPort(
            IncidenteRepositoryPort incidenteRepo,
            AsignacionRepositoryPort asignacionRepo,
            EventPublisherPort eventPublisher) {
        return new MarcarAgenteEnCaminoService(incidenteRepo, asignacionRepo, eventPublisher);
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
 
    // ── Token FCM ─────────────────────────────────────────────────────────
 
    /**
     * Caso de uso: registrar o actualizar el token FCM del denunciante.
     * Necesario para que Firebase envíe notificaciones push al dispositivo.
     */
    @Bean
    public RegistrarTokenFcmPort registrarTokenFcmPort(
            DenuncianteRepositoryPort denuncianteRepo) {
        return new com.callsos.backend.application.service.RegistrarTokenFcmService(denuncianteRepo);
    }

    // ── Consultas (Fase E) ─────────────────────────────────────────────────

    @Bean
    public com.callsos.backend.domain.port.in.ConsultarIncidentePort consultarIncidentePort(
            com.callsos.backend.domain.port.out.IncidenteRepositoryPort incidenteRepo) {
        return new com.callsos.backend.application.service.ConsultarIncidenteService(incidenteRepo);
    }

    @Bean
    public com.callsos.backend.domain.port.in.ConsultarMisIncidentesPort consultarMisIncidentesPort(
            com.callsos.backend.domain.port.out.IncidenteRepositoryPort incidenteRepo) {
        return new com.callsos.backend.application.service.ConsultarMisIncidentesService(incidenteRepo);
    }

    @Bean
    public com.callsos.backend.domain.port.in.ConsultarIncidentesAsignadosPort consultarIncidentesAsignadosPort(
            com.callsos.backend.domain.port.out.IncidenteRepositoryPort incidenteRepo) {
        return new com.callsos.backend.application.service.ConsultarIncidentesAsignadosService(incidenteRepo);
    }

    @Bean
    public com.callsos.backend.domain.port.in.ConsultarIncidentesPorCAIPort consultarIncidentesPorCAIPort(
            com.callsos.backend.domain.port.out.IncidenteRepositoryPort incidenteRepo) {
        return new com.callsos.backend.application.service.ConsultarIncidentesPorCAIService(incidenteRepo);
    }
}