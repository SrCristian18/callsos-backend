package com.callsos.backend.infrastructure.adapter.out.event;

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.event.AgenteEnCaminoEvent;
import com.callsos.backend.domain.event.IncidenteEvent;
import com.callsos.backend.domain.event.IncidenteFinalizadoEvent;
import com.callsos.backend.domain.event.TipoIncidenteActualizadoEvent;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UnidadPolicial;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.NotificacionPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionEventListener")
class NotificacionEventListenerTest {

    @Mock NotificacionPort notificacionPort;
    @Mock DenuncianteRepositoryPort denuncianteRepository;
    @Mock IncidenteRepositoryPort incidenteRepository;
    @Mock AsignacionRepositoryPort asignacionRepository;

    NotificacionEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificacionEventListener(
            notificacionPort, denuncianteRepository, incidenteRepository, asignacionRepository);
    }

    @Test
    @DisplayName("onAgenteEnCamino notifica al denunciante")
    void onAgenteEnCamino() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));

        listener.onAgenteEnCamino(new AgenteEnCaminoEvent(
            "i-001", "den-001", EstadoIncidente.AGENTE_ASIGNADO, "ag-001"));

        verify(notificacionPort).notificarDenunciante(eq(denunciante), any());
    }

    @Test
    @DisplayName("onIncidenteFinalizado notifica según FINALIZADO/CANCELADO")
    void onIncidenteFinalizado() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));

        listener.onIncidenteFinalizado(new IncidenteFinalizadoEvent(
            "i-001", "den-001", EstadoIncidente.EN_ATENCION, EstadoIncidente.FINALIZADO));

        verify(notificacionPort).notificarDenunciante(
            eq(denunciante), eq("Tu incidente ha sido atendido exitosamente."));
    }

    @Test
    @DisplayName("onTipoActualizado notifica al agente asignado y al CAI")
    void onTipoActualizadoNotificaAgenteYCai() {
        UnidadPolicial cai = new UnidadPolicial(
            "cai-001", "CAI Test", "Calle 1", new Ubicacion(10.4, -75.5), "6010000");
        Agente agente = new Agente(
            "ag-001", "Pedro", "Av. Test", new Ubicacion(10.4, -75.5), "3002222222");
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");

        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.RIÑAS_O_PELEAS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        incidente.reconstituirUnidad(cai);

        Asignacion asignacion = Asignacion.reconstituir(
            "asig-001", LocalDateTime.now(),
            com.callsos.backend.domain.enums.EstadoAsignacion.ACTIVA, agente, null);

        when(asignacionRepository.buscarPorIncidente("i-001")).thenReturn(Optional.of(asignacion));
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        listener.onTipoActualizado(new TipoIncidenteActualizadoEvent(
            "i-001", "den-001", EstadoIncidente.DERIVADO_A_CAI,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS));

        verify(notificacionPort).notificarAgente(eq(agente), any());
        verify(notificacionPort).notificarUnidadPolicial(eq(cai), any());
    }

    @Test
    @DisplayName("onTipoActualizado sin agente asignado todavía no falla, solo omite esa notificación")
    void onTipoActualizadoSinAgenteAsignado() {
        UnidadPolicial cai = new UnidadPolicial(
            "cai-001", "CAI Test", "Calle 1", new Ubicacion(10.4, -75.5), "6010000");
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");

        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.RIÑAS_O_PELEAS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        incidente.reconstituirUnidad(cai);

        when(asignacionRepository.buscarPorIncidente("i-001")).thenReturn(Optional.empty());
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        listener.onTipoActualizado(new TipoIncidenteActualizadoEvent(
            "i-001", "den-001", EstadoIncidente.DERIVADO_A_CAI,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS));

        verify(notificacionPort, never()).notificarAgente(any(), any());
        verify(notificacionPort).notificarUnidadPolicial(eq(cai), any());
    }

    @Test
    @DisplayName("onTipoActualizado sin CAI derivado todavía no falla, solo omite esa notificación")
    void onTipoActualizadoSinCai() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");

        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.RIÑAS_O_PELEAS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        // sin reconstituirUnidad(): unidadPolicial queda null (recién CREADO)

        when(asignacionRepository.buscarPorIncidente("i-001")).thenReturn(Optional.empty());
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        listener.onTipoActualizado(new TipoIncidenteActualizadoEvent(
            "i-001", "den-001", EstadoIncidente.CREADO,
            TipoIncidente.ROBOS_O_ASALTOS, TipoIncidente.RIÑAS_O_PELEAS));

        verify(notificacionPort, never()).notificarAgente(any(), any());
        verify(notificacionPort, never()).notificarUnidadPolicial(any(), any());
    }

    @Test
    @DisplayName("AUD-5: onIncidenteEvent(DERIVADO_A_CAI) notifica a la unidad")
    void onIncidenteEventDerivadoACaiNotificaUnidad() {
        UnidadPolicial cai = new UnidadPolicial(
            "cai-001", "CAI Test", "Calle 1", new Ubicacion(10.4, -75.5), "6010000");
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.RIÑAS_O_PELEAS, "desc",
            new Ubicacion(10.4, -75.5), denunciante);
        incidente.reconstituirUnidad(cai);

        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        listener.onIncidenteEvent(new IncidenteEvent(
            "i-001", "den-001", EstadoIncidente.CREADO, EstadoIncidente.DERIVADO_A_CAI));

        verify(notificacionPort).notificarUnidadPolicial(eq(cai), any());
    }

    @Test
    @DisplayName("AUD-5: onIncidenteEvent(AGENTE_ASIGNADO) notifica al agente")
    void onIncidenteEventAgenteAsignadoNotificaAgente() {
        Agente agente = new Agente(
            "ag-001", "Pedro", "Av. Test", new Ubicacion(10.4, -75.5), "3002222222");
        Asignacion asignacion = Asignacion.reconstituir(
            "asig-001", LocalDateTime.now(),
            com.callsos.backend.domain.enums.EstadoAsignacion.ACTIVA, agente, null);

        when(asignacionRepository.buscarPorIncidente("i-001")).thenReturn(Optional.of(asignacion));

        listener.onIncidenteEvent(new IncidenteEvent(
            "i-001", "den-001", EstadoIncidente.DERIVADO_A_CAI, EstadoIncidente.AGENTE_ASIGNADO));

        verify(notificacionPort).notificarAgente(eq(agente), any());
    }

    @Test
    @DisplayName("AUD-5: onIncidenteEvent(EN_ATENCION) notifica al denunciante")
    void onIncidenteEventEnAtencionNotificaDenunciante() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));

        listener.onIncidenteEvent(new IncidenteEvent(
            "i-001", "den-001", EstadoIncidente.AGENTE_EN_CAMINO, EstadoIncidente.EN_ATENCION));

        verify(notificacionPort).notificarDenunciante(eq(denunciante), any());
    }

    @Test
    @DisplayName("AUD-5: onIncidenteEvent(CREADO) no notifica a nadie")
    void onIncidenteEventCreadoNoNotificaNada() {
        listener.onIncidenteEvent(new IncidenteEvent(
            "i-001", "den-001", null, EstadoIncidente.CREADO));

        verify(notificacionPort, never()).notificarAgente(any(), any());
        verify(notificacionPort, never()).notificarUnidadPolicial(any(), any());
        verify(notificacionPort, never()).notificarDenunciante(any(), any());
    }

    @Test
    @DisplayName("AUD-5 (regresión): onIncidenteFinalizado(CANCELADO) también notifica al último agente asignado")
    void onIncidenteFinalizadoCanceladoNotificaAgente() {
        // Antes del fix, este escenario (denunciante cancela mientras el
        // agente ya iba en camino) dejaba al agente sin ningún aviso — el
        // agente seguía manejando hacia un incidente ya cancelado.
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));

        Agente agente = new Agente(
            "ag-001", "Pedro", "Av. Test", new Ubicacion(10.4, -75.5), "3002222222");
        // FINALIZADA a propósito: simula que AgenteLiberador ya corrió
        // antes de que se publicara el evento (ver Javadoc del método).
        Asignacion asignacionYaLiberada = Asignacion.reconstituir(
            "asig-001", LocalDateTime.now(),
            com.callsos.backend.domain.enums.EstadoAsignacion.FINALIZADA, agente, null);
        when(asignacionRepository.buscarUltimaPorIncidente("i-001"))
            .thenReturn(Optional.of(asignacionYaLiberada));

        listener.onIncidenteFinalizado(new IncidenteFinalizadoEvent(
            "i-001", "den-001", EstadoIncidente.AGENTE_EN_CAMINO, EstadoIncidente.CANCELADO));

        verify(notificacionPort).notificarDenunciante(eq(denunciante), eq("Tu incidente ha sido cancelado."));
        verify(notificacionPort).notificarAgente(eq(agente), any());
    }

    @Test
    @DisplayName("onIncidenteFinalizado(FINALIZADO) NO notifica a ningún agente (ya lo sabe: fue quien lo finalizó)")
    void onIncidenteFinalizadoFinalizadoNoNotificaAgente() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));

        listener.onIncidenteFinalizado(new IncidenteFinalizadoEvent(
            "i-001", "den-001", EstadoIncidente.EN_ATENCION, EstadoIncidente.FINALIZADO));

        verify(notificacionPort, never()).notificarAgente(any(), any());
        verify(asignacionRepository, never()).buscarUltimaPorIncidente(any());
    }

    @Test
    @DisplayName("AUD-5 (regresión): onIncidenteFinalizado(CANCELADO) sin agente asignado no falla")
    void onIncidenteFinalizadoCanceladoSinAgente() {
        Denunciante denunciante = new Denunciante(
            "den-001", "Juan", "Cartagena", "3001111111", "juan@test.com");
        when(denuncianteRepository.buscarPorId("den-001")).thenReturn(Optional.of(denunciante));
        when(asignacionRepository.buscarUltimaPorIncidente("i-001")).thenReturn(Optional.empty());

        listener.onIncidenteFinalizado(new IncidenteFinalizadoEvent(
            "i-001", "den-001", EstadoIncidente.CREADO, EstadoIncidente.CANCELADO));

        verify(notificacionPort, never()).notificarAgente(any(), any());
    }
}