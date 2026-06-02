/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */

import com.callsos.backend.domain.enums.EstadoIncidente;
import com.callsos.backend.domain.enums.TipoIncidente;
import com.callsos.backend.domain.valueobject.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
 
import static org.junit.jupiter.api.Assertions.*;
 
/**
 * Tests del agregado raíz Incidente.
 * El dominio no tiene dependencias externas — cero mocks necesarios.
 * Cada test verifica reglas de negocio puras.
 */
@DisplayName("Incidente — agregado raíz")
class IncidenteTest {
     private Incidente incidente;
    private Denunciante denunciante;
    private Ubicacion ubicacion;
 
    @BeforeEach
    void setUp() {
        ubicacion   = new Ubicacion(10.39, -75.51);
        denunciante = new Denunciante("d-001", "Juan Pérez", "Cartagena",
                                     "3001234567", "juan@test.com");
        incidente   = new Incidente("i-001", TipoIncidente.ROBOS_O_ASALTOS,
                                    "Robo en la calle", ubicacion, denunciante);
    }
 
    @Test
    @DisplayName("Nace en estado CREADO")
    void naceCREADO() {
        assertEquals(EstadoIncidente.CREADO, incidente.getEstado());
    }
 
    @Test
    @DisplayName("Tiene los atributos del constructor correctos")
    void atributosIniciales() {
        assertEquals("i-001", incidente.getId());
        assertEquals(TipoIncidente.ROBOS_O_ASALTOS, incidente.getTipo());
        assertEquals(denunciante, incidente.getDenunciante());
        assertNotNull(incidente.getFechaHora());
        assertTrue(incidente.getAsignaciones().isEmpty());
    }
 
    @Nested
    @DisplayName("Máquina de estados — transiciones válidas")
    class TransicionesValidas {
 
        @Test @DisplayName("CREADO → DERIVADO_A_CAI")
        void creadoADerivado() {
            UnidadPolicial cai = caiDePrueba();
            incidente.derivarACAI(cai);
            assertEquals(EstadoIncidente.DERIVADO_A_CAI, incidente.getEstado());
            assertEquals(cai, incidente.getUnidadPolicial());
        }
 
        @Test @DisplayName("DERIVADO_A_CAI → AGENTE_ASIGNADO")
        void derivadoAAsignado() {
            incidente.derivarACAI(caiDePrueba());
            incidente.marcarAgenteAsignado();
            assertEquals(EstadoIncidente.AGENTE_ASIGNADO, incidente.getEstado());
        }
 
        @Test @DisplayName("AGENTE_ASIGNADO → AGENTE_EN_CAMINO")
        void asignadoAEnCamino() {
            incidente.derivarACAI(caiDePrueba());
            incidente.marcarAgenteAsignado();
            incidente.marcarAgenteEnCamino();
            assertEquals(EstadoIncidente.AGENTE_EN_CAMINO, incidente.getEstado());
        }
 
        @Test @DisplayName("AGENTE_EN_CAMINO → EN_ATENCION")
        void enCaminoAEnAtencion() {
            llevarAEstado(EstadoIncidente.AGENTE_EN_CAMINO);
            incidente.iniciarAtencion();
            assertEquals(EstadoIncidente.EN_ATENCION, incidente.getEstado());
        }
 
        @Test @DisplayName("EN_ATENCION → FINALIZADO")
        void enAtencionAFinalizado() {
            llevarAEstado(EstadoIncidente.EN_ATENCION);
            incidente.finalizar();
            assertEquals(EstadoIncidente.FINALIZADO, incidente.getEstado());
        }
 
        @Test @DisplayName("CREADO → CANCELADO (denunciante cancela)")
        void cancelarDesdeCREADO() {
            incidente.cancelar();
            assertEquals(EstadoIncidente.CANCELADO, incidente.getEstado());
        }
 
        @Test @DisplayName("AGENTE_EN_CAMINO → CANCELADO")
        void cancelarDesdeEnCamino() {
            llevarAEstado(EstadoIncidente.AGENTE_EN_CAMINO);
            incidente.cancelar();
            assertEquals(EstadoIncidente.CANCELADO, incidente.getEstado());
        }
    }
 
    @Nested
    @DisplayName("Máquina de estados — transiciones INVÁLIDAS")
    class TransicionesInvalidas {
 
        @Test @DisplayName("CREADO no puede saltar a AGENTE_ASIGNADO")
        void noSaltarEstados() {
            assertThrows(IllegalStateException.class,
                () -> incidente.marcarAgenteAsignado());
        }
 
        @Test @DisplayName("FINALIZADO es estado terminal")
        void finalizadoEsTerminal() {
            llevarAEstado(EstadoIncidente.FINALIZADO);
            assertThrows(IllegalStateException.class, () -> incidente.cancelar());
        }
 
        @Test @DisplayName("CANCELADO es estado terminal")
        void canceladoEsTerminal() {
            incidente.cancelar();
            assertThrows(IllegalStateException.class,
                () -> incidente.derivarACAI(caiDePrueba()));
        }
 
        @Test @DisplayName("No se puede asignar un CAI dos veces")
        void noReasignarCAI() {
            incidente.derivarACAI(caiDePrueba());
            assertThrows(IllegalStateException.class,
                () -> incidente.derivarACAI(caiDePrueba()));
        }
    }
 
    @Nested
    @DisplayName("Reglas de negocio")
    class ReglasNegocio {
 
        @Test @DisplayName("setDenuncia solo puede llamarse una vez")
        void denunciaInmutableTrasAsignar() {
            Denuncia d = new Denuncia("den-001", TipoIncidente.ROBOS_O_ASALTOS,
                "desc", ubicacion, denunciante, incidente);
            incidente.setDenuncia(d);
            assertThrows(IllegalStateException.class,
                () -> incidente.setDenuncia(d));
        }
 
        @Test @DisplayName("reconstituirEstado no dispara reglas de negocio")
        void reconstitucionSinReglas() {
            incidente.reconstituirEstado(EstadoIncidente.EN_ATENCION);
            assertEquals(EstadoIncidente.EN_ATENCION, incidente.getEstado());
        }
    }
 
    // ── Helpers ────────────────────────────────────────────────────────────
 
    private void llevarAEstado(EstadoIncidente objetivo) {
        if (objetivo == EstadoIncidente.CREADO) return;
        incidente.derivarACAI(caiDePrueba());
        if (objetivo == EstadoIncidente.DERIVADO_A_CAI) return;
        incidente.marcarAgenteAsignado();
        if (objetivo == EstadoIncidente.AGENTE_ASIGNADO) return;
        incidente.marcarAgenteEnCamino();
        if (objetivo == EstadoIncidente.AGENTE_EN_CAMINO) return;
        incidente.iniciarAtencion();
        if (objetivo == EstadoIncidente.EN_ATENCION) return;
        incidente.finalizar();
    }
 
    private UnidadPolicial caiDePrueba() {
        return new UnidadPolicial("cai-001", "CAI Manga",
            "Calle 10", ubicacion, "6010000");
    }
}
