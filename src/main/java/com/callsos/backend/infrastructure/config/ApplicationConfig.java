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
import com.callsos.backend.infrastructure.adapter.out.ruta.SimulacionEstado;
import com.callsos.backend.infrastructure.config.security.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

    @Bean
    public RegistrarDenunciantePort registrarDenunciantePort(
            DenuncianteRepositoryPort denuncianteRepo,
            UsuarioRepositoryPort usuarioRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        return new RegistrarDenuncianteService(
            denuncianteRepo, usuarioRepo, passwordEncoder, jwtService);
    }

    @Bean
    public RegistrarAgenteConInvitacionPort registrarAgenteConInvitacionPort(
            InvitacionAgenteRepositoryPort invitacionRepo,
            AgenteRepositoryPort agenteRepo,
            UsuarioRepositoryPort usuarioRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        return new RegistrarAgenteConInvitacionService(
            invitacionRepo, agenteRepo, usuarioRepo, passwordEncoder, jwtService);
    }

    @Bean
    public GenerarInvitacionAgentePort generarInvitacionAgentePort(
            InvitacionAgenteRepositoryPort invitacionRepo) {
        return new GenerarInvitacionAgenteService(invitacionRepo);
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

    @Bean
    public ConsultarIncidentesPorEstadoPort consultarIncidentesPorEstadoPort(
            IncidenteRepositoryPort incidenteRepo) {
        return new ConsultarIncidentesPorEstadoService(incidenteRepo);
    }

    /**
     * Épica 1 — el denunciante actualiza el tipo de su incidente mientras
     * está activo. Ownership + regla de estado se validan dentro del
     * servicio/agregado, no aquí.
     */
    @Bean
    public ActualizarTipoIncidentePort actualizarTipoIncidentePort(
            IncidenteRepositoryPort incidenteRepo) {
        return new ActualizarTipoIncidenteService(incidenteRepo);
    }

    //Tracking GPS - Simulacion de recorrido (SOLO para pruebas piloto)
    @Bean
    public PublicarUbicacionAgentePort publicarUbicacionAgentePort(
        UbicacionAgenteRepositoryPort ubicacionAgenteRepo,
        SimpMessagingTemplate messagingTemplate){
            return new PublicarUbicacionAgenteService(ubicacionAgenteRepo, messagingTemplate);
        }

    /**
     * Scheduler dedicado para las tareas periódicas de simulación de
     * recorrido. Pool pequeño: cada piloto solo corre unas pocas
     * simulaciones concurrentes a la vez.
     */

    @Bean
    public TaskScheduler taskScheduler(){
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sim-recorrido-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public SimularRecorridoAgentePort simularRecorridoAgentePort(
        IncidenteRepositoryPort incidenteRepo,
        AsignacionRepositoryPort asignacionrepo,
        RutaPort rutarPort,
        PublicarUbicacionAgentePort publicarUbicacion,
        SimulacionEstado simulacionEstado,
        TaskScheduler taskScheduler,
        @Value("${SIMULACION_VELOCIDAD:35}") double velocidadKmh,
        @Value("${SIMULACION_INTERVALO_MS:2000}") long intervaloMs)
        {
            return new SimularRecorridoAgenteService(
                incidenteRepo, asignacionrepo, rutarPort,
                publicarUbicacion, simulacionEstado, taskScheduler,
                velocidadKmh, intervaloMs);
        }
    @Bean
    public ConsultarAgentesDisponiblesPorCaiPort consultarAgentesDisponiblesPorCaiPort(
            AgenteRepositoryPort agenteRepo) {
        return new ConsultarAgentesDisponiblesPorCaiService(agenteRepo);
    }
}