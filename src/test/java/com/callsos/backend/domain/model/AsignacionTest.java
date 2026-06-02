/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoAgente;
import com.callsos.backend.domain.enums.EstadoAsignacion;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
 
import java.time.LocalDateTime;
 
import static org.junit.jupiter.api.Assertions.*;
 
@DisplayName("Asignacion — clase ternaria")
class AsignacionTest {
     private final Ubicacion ubicacion = new Ubicacion(10.39, -75.51);
 
    @Test @DisplayName("Creación ocupa al agente automáticamente")
    void creacionOcupaAgente() {
        Agente agente = agenteDePrueba();
        assertTrue(agente.estaDisponible());
 
        Asignacion a = new Asignacion("a-001", agente, denunciaDePrueba(agente));
        assertEquals(EstadoAsignacion.ACTIVA, a.getEstado());
        assertFalse(agente.estaDisponible());  // efecto de dominio
    }
 
    @Test @DisplayName("No se puede crear con agente OCUPADO")
    void noCrearConAgenteOcupado() {
        Agente agente = agenteDePrueba();
        agente.asignar();
        assertThrows(IllegalStateException.class,
            () -> new Asignacion("a-001", agente, denunciaDePrueba(agente)));
    }
 
    @Test @DisplayName("finalizar() pone FINALIZADO y libera al agente")
    void finalizarLiberaAgente() {
        Agente agente = agenteDePrueba();
        Asignacion a  = new Asignacion("a-001", agente, denunciaDePrueba(agente));
        a.finalizar();
        assertEquals(EstadoAsignacion.FINALIZADA, a.getEstado());
        assertTrue(agente.estaDisponible());
    }
 
    @Test @DisplayName("reconstituir() no dispara efectos de dominio")
    void reconstituirSinEfectos() {
        Agente agente = agenteDePrueba();
        agente.asignar();  // ya está ocupado, como viene de BD
 
        // No lanza excepción aunque el agente esté OCUPADO
        Asignacion a = Asignacion.reconstituir(
            "a-001", LocalDateTime.now(),
            EstadoAsignacion.ACTIVA, agente, null);
 
        assertNotNull(a);
        assertEquals(EstadoAsignacion.ACTIVA, a.getEstado());
        assertEquals(EstadoAgente.OCUPADO, agente.getEstado()); // sin doble asignación
    }
 
    @Test @DisplayName("No se puede finalizar dos veces")
    void noDobleFinalizar() {
        Agente agente = agenteDePrueba();
        Asignacion a  = new Asignacion("a-001", agente, denunciaDePrueba(agente));
        a.finalizar();
        assertThrows(IllegalStateException.class, a::finalizar);
    }
 
    // ── Helpers ────────────────────────────────────────────────────────────
 
    private Agente agenteDePrueba() {
        return new Agente("ag-001", "Pedro Ruiz",
            "Av. Principal", ubicacion, "3009876543");
    }
 
    private Denuncia denunciaDePrueba(Agente agente) {
        Denunciante denunciante = new Denunciante("d-001", "Juan",
            "Cartagena", "300", "j@test.com");
        Incidente incidente = new Incidente("i-001", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante);
        return new Denuncia("den-001", TipoIncidente.ROBOS_O_ASALTOS,
            "desc", ubicacion, denunciante, incidente);
    }
}
