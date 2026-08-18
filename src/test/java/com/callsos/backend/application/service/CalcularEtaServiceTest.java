/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.CategoriaDistancia;
import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.exception.AccesoDenegadoException;
import com.callsos.backend.domain.model.Agente;
import com.callsos.backend.domain.model.Asignacion;
import com.callsos.backend.domain.model.Denuncia;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.model.EtaInfo;
import com.callsos.backend.domain.model.Incidente;
import com.callsos.backend.domain.model.UbicacionAgente;
import com.callsos.backend.domain.port.out.AsignacionRepositoryPort;
import com.callsos.backend.domain.port.out.IncidenteRepositoryPort;
import com.callsos.backend.domain.port.out.UbicacionAgenteRepositoryPort;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Épica 4 — ETA seguro para el denunciante.
 *
 * Casos exigidos explícitamente por la épica: sin posición conocida, con
 * posición, redondeo, y no exposición de lat/lon en el payload (este
 * último se verifica "por construcción": EtaInfo no tiene getters de
 * lat/lon — ver el test dedicado más abajo, que documenta la garantía
 * a nivel de tipo, no solo de valor).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalcularEtaService")
class CalcularEtaServiceTest {

    @Mock IncidenteRepositoryPort incidenteRepository;
    @Mock AsignacionRepositoryPort asignacionRepository;
    @Mock UbicacionAgenteRepositoryPort ubicacionAgenteRepository;

    private static final double VELOCIDAD_MEDIA_KMH = 36.0; // 10 m/s exactos, fácil de razonar

    CalcularEtaService service;

    private final Ubicacion ubicacionIncidente = new Ubicacion(10.400, -75.500);
    private final Denunciante denunciante = new Denunciante(
        "den-001", "Juan Test", "Cartagena", "3001111111", "juan@test.com");

    @BeforeEach
    void setUp() {
        service = new CalcularEtaService(
            incidenteRepository, asignacionRepository, ubicacionAgenteRepository, VELOCIDAD_MEDIA_KMH);
    }

    private Incidente incidenteEnCamino() {
        Incidente incidente = new Incidente(
            "i-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacionIncidente, denunciante);
        incidente.reconstituirEstado(EstadoIncidente.AGENTE_EN_CAMINO);
        return incidente;
    }

    private Asignacion asignacionCon(String agenteId) {
        Ubicacion u = new Ubicacion(10.0, -75.0);
        Agente agente = new Agente(agenteId, "Pedro", "Dir", u, "300");
        Denuncia denuncia = new Denuncia(
            "den-reg-001", TipoIncidente.ROBOS_O_ASALTOS, "desc", u, denunciante, incidenteEnCamino());
        return Asignacion.reconstituir(
            "as-001", LocalDateTime.now(),
            com.callsos.backend.domain.enums.EstadoAsignacion.ACTIVA, agente, denuncia);
    }

    @Test
    @DisplayName("incidente inexistente lanza IllegalArgumentException (404)")
    void incidenteNoEncontrado() {
        when(incidenteRepository.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.consultar("no-existe", "den-001"));
    }

    @Test
    @DisplayName("denunciante ajeno (no dueño) recibe AccesoDenegadoException (403)")
    void denuncianteAjenoRechazado() {
        Incidente incidente = incidenteEnCamino();
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));

        assertThrows(AccesoDenegadoException.class,
            () -> service.consultar("i-001", "den-999-otro"));

        verifyNoInteractions(asignacionRepository, ubicacionAgenteRepository);
    }

    @Test
    @DisplayName("incidente que aún no está AGENTE_EN_CAMINO retorna EtaInfo.sinDatos()")
    void incidenteNoEnCaminoRetornaSinDatos() {
        Incidente incidente = new Incidente(
            "i-002", TipoIncidente.ROBOS_O_ASALTOS, "desc", ubicacionIncidente, denunciante);
        incidente.reconstituirEstado(EstadoIncidente.AGENTE_ASIGNADO);
        when(incidenteRepository.buscarPorId("i-002")).thenReturn(Optional.of(incidente));

        EtaInfo eta = service.consultar("i-002", "den-001");

        assertFalse(eta.tieneDatos());
        assertNull(eta.getMinutosEstimados());
        assertNull(eta.getCategoriaDistancia());
        verifyNoInteractions(ubicacionAgenteRepository);
    }

    @Test
    @DisplayName("sin posición conocida del agente (aún no reportó GPS) retorna EtaInfo.sinDatos()")
    void sinPosicionConocidaRetornaSinDatos() {
        Incidente incidente = incidenteEnCamino();
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepository.buscarPorIncidente("i-001"))
            .thenReturn(Optional.of(asignacionCon("ag-001")));
        when(ubicacionAgenteRepository.ultimaPosicion("ag-001", "i-001"))
            .thenReturn(Optional.empty());

        EtaInfo eta = service.consultar("i-001", "den-001");

        assertFalse(eta.tieneDatos());
        assertNull(eta.getMinutosEstimados());
        assertNull(eta.getCategoriaDistancia());
    }

    @Test
    @DisplayName("sin asignación activa retorna EtaInfo.sinDatos() sin consultar posición")
    void sinAsignacionRetornaSinDatos() {
        Incidente incidente = incidenteEnCamino();
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepository.buscarPorIncidente("i-001")).thenReturn(Optional.empty());

        EtaInfo eta = service.consultar("i-001", "den-001");

        assertFalse(eta.tieneDatos());
        verifyNoInteractions(ubicacionAgenteRepository);
    }

    @Test
    @DisplayName("con posición conocida calcula minutos (redondeados hacia arriba) y categoría de distancia")
    void conPosicionConocidaCalculaEta() {
        Incidente incidente = incidenteEnCamino(); // destino: 10.400, -75.500
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepository.buscarPorIncidente("i-001"))
            .thenReturn(Optional.of(asignacionCon("ag-001")));

        // Posición del agente a ~556m del destino (0.005° de latitud) —
        // a 36 km/h (10 m/s) exactos, ~55.6s -> redondea hacia arriba a 1 minuto.
        Ubicacion posicionAgente = new Ubicacion(10.405, -75.500);
        UbicacionAgente ultima = new UbicacionAgente("ag-001", "i-001", posicionAgente);
        when(ubicacionAgenteRepository.ultimaPosicion("ag-001", "i-001"))
            .thenReturn(Optional.of(ultima));

        EtaInfo eta = service.consultar("i-001", "den-001");

        assertTrue(eta.tieneDatos());
        assertEquals(1, eta.getMinutosEstimados());
        assertEquals(CategoriaDistancia.MENOS_DE_1_KM, eta.getCategoriaDistancia());
    }

    @Test
    @DisplayName("redondeo hacia arriba: una distancia que da minutos fraccionarios nunca se trunca hacia abajo")
    void redondeoSiempreHaciaArriba() {
        Incidente incidente = incidenteEnCamino(); // destino: 10.400, -75.500
        when(incidenteRepository.buscarPorId("i-001")).thenReturn(Optional.of(incidente));
        when(asignacionRepository.buscarPorIncidente("i-001"))
            .thenReturn(Optional.of(asignacionCon("ag-001")));

        // ~2.2km al norte -> a 36km/h (10 m/s) son ~220s = 3.67 min -> debe redondear a 4, no a 3.
        Ubicacion posicionAgente = new Ubicacion(10.420, -75.500);
        UbicacionAgente ultima = new UbicacionAgente("ag-001", "i-001", posicionAgente);
        when(ubicacionAgenteRepository.ultimaPosicion("ag-001", "i-001"))
            .thenReturn(Optional.of(ultima));

        EtaInfo eta = service.consultar("i-001", "den-001");

        assertTrue(eta.tieneDatos());
        assertTrue(eta.getMinutosEstimados() >= 4,
            "un tiempo fraccionario (~3.67 min) debe redondearse hacia arriba, nunca truncarse");
        assertEquals(CategoriaDistancia.ENTRE_1_Y_3_KM, eta.getCategoriaDistancia());
    }

    @Test
    @DisplayName("el payload de ETA nunca expone coordenadas — EtaInfo no tiene getters de lat/lon")
    void etaInfoNuncaExponeCoordenadas() {
        // Garantía estructural, no solo de valor: si alguien agregara
        // getLatitud()/getLongitud() a EtaInfo en el futuro, este test
        // documentaría por qué NO debe hacerse, y fallaría al reflexionar
        // sobre los métodos públicos de la clase.
        long metodosSospechosos = java.util.Arrays.stream(EtaInfo.class.getMethods())
            .map(java.lang.reflect.Method::getName)
            .filter(nombre -> nombre.toLowerCase().contains("lat")
                || nombre.toLowerCase().contains("lon")
                || nombre.toLowerCase().contains("ubicacion")
                || nombre.toLowerCase().contains("coordenada"))
            .count();

        assertEquals(0, metodosSospechosos,
            "EtaInfo no debe exponer ningún método relacionado a coordenadas");
    }
}