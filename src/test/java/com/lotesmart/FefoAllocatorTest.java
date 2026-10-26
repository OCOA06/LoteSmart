package com.lotesmart;

import com.lotesmart.modelo.Asignacion;
import com.lotesmart.modelo.LoteDisponible;
import com.lotesmart.modelo.Producto;
import com.lotesmart.negocio.FefoAllocator;
import com.lotesmart.negocio.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pruebas Unitarias del Algoritmo FEFO (First Expired, First Out)")
class FefoAllocatorTest {

    private FefoAllocator fefoAllocator;
    private Producto productoLeche;
    private Producto productoParacetamol;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        fefoAllocator = new FefoAllocator();
        hoy = LocalDate.of(2026, 10, 25);

        // Producto 1: Leche - requiere al menos 7 días de caducidad
        productoLeche = new Producto(1, "LEC-1001", "Leche entera 1 L", "LITRO", new BigDecimal("26.50"), 50, 7, true);

        // Producto 2: Paracetamol - requiere al menos 30 días de caducidad
        productoParacetamol = new Producto(2, "MED-2001", "Paracetamol 500mg", "CAJA", new BigDecimal("45.00"), 30, 30, true);
    }

    @Test
    @DisplayName("Debe despachar primero el lote con fecha de caducidad más próxima (FEFO puro)")
    void testDespachoFefoBasico() throws ReglaNegocioException {
        List<LoteDisponible> lotes = new ArrayList<>();
        // Lote 2 vence en 45 días
        lotes.add(new LoteDisponible(2, 1, "LG-2411-C", hoy.plusDays(45), 100, new BigDecimal("19.50")));
        // Lote 1 vence en 14 días
        lotes.add(new LoteDisponible(1, 1, "LG-2410-A", hoy.plusDays(14), 35, new BigDecimal("18.50")));
        // Lote 3 vence en 20 días
        lotes.add(new LoteDisponible(3, 1, "LG-2410-B", hoy.plusDays(20), 50, new BigDecimal("19.00")));

        // Solicitamos 20 unidades: deben salir 100% del lote que vence primero (LG-2410-A a 14 días)
        List<Asignacion> asignaciones = fefoAllocator.asignarLotes(productoLeche, lotes, 20, hoy);

        assertNotNull(asignaciones);
        assertEquals(1, asignaciones.size());
        assertEquals("LG-2410-A", asignaciones.get(0).getCodigoLote());
        assertEquals(20, asignaciones.get(0).getCantidad());
    }

    @Test
    @DisplayName("Debe repartir el pedido entre múltiples lotes si el primero no cubre la cantidad solicitada")
    void testRepartoEntreVariosLotes() throws ReglaNegocioException {
        List<LoteDisponible> lotes = new ArrayList<>();
        lotes.add(new LoteDisponible(1, 1, "LG-2410-A", hoy.plusDays(14), 35, new BigDecimal("18.50")));
        lotes.add(new LoteDisponible(2, 1, "LG-2410-B", hoy.plusDays(20), 50, new BigDecimal("19.00")));
        lotes.add(new LoteDisponible(3, 1, "LG-2411-C", hoy.plusDays(45), 100, new BigDecimal("19.50")));

        // Solicitamos 60 unidades: deben salir 35 de LG-2410-A y 25 de LG-2410-B
        List<Asignacion> asignaciones = fefoAllocator.asignarLotes(productoLeche, lotes, 60, hoy);

        assertEquals(2, asignaciones.size());
        assertEquals("LG-2410-A", asignaciones.get(0).getCodigoLote());
        assertEquals(35, asignaciones.get(0).getCantidad());

        assertEquals("LG-2410-B", asignaciones.get(1).getCodigoLote());
        assertEquals(25, asignaciones.get(1).getCantidad());
    }

    @Test
    @DisplayName("En caso de empate en fecha de caducidad, debe desempatar por idLote ASC (el más antiguo primero)")
    void testDesempatePorIdLote() throws ReglaNegocioException {
        List<LoteDisponible> lotes = new ArrayList<>();
        LocalDate mismaCaducidad = hoy.plusDays(20);
        lotes.add(new LoteDisponible(105, 1, "LOTE-NUEVO", mismaCaducidad, 50, new BigDecimal("20.00")));
        lotes.add(new LoteDisponible(101, 1, "LOTE-ANTIGUO", mismaCaducidad, 50, new BigDecimal("20.00")));

        List<Asignacion> asignaciones = fefoAllocator.asignarLotes(productoLeche, lotes, 30, hoy);

        assertEquals(1, asignaciones.size());
        assertEquals(101, asignaciones.get(0).getIdLote());
        assertEquals("LOTE-ANTIGUO", asignaciones.get(0).getCodigoLote());
        assertEquals(30, asignaciones.get(0).getCantidad());
    }

    @Test
    @DisplayName("Debe ignorar lotes que no cumplan con la política de días mínimos de venta")
    void testExclusionPorDiasMinimosVenta() throws ReglaNegocioException {
        // Paracetamol requiere 30 días mínimos de vida útil
        List<LoteDisponible> lotes = new ArrayList<>();
        // Lote a 25 días (< 30 días mínimos: no debe asignarse aunque esté vigente)
        lotes.add(new LoteDisponible(10, 2, "PA-77812", hoy.plusDays(25), 40, new BigDecimal("22.00")));
        // Lote a 90 días (cumple la política)
        lotes.add(new LoteDisponible(11, 2, "PA-88901", hoy.plusDays(90), 80, new BigDecimal("24.00")));

        List<Asignacion> asignaciones = fefoAllocator.asignarLotes(productoParacetamol, lotes, 15, hoy);

        assertEquals(1, asignaciones.size());
        assertEquals("PA-88901", asignaciones.get(0).getCodigoLote());
        assertEquals(15, asignaciones.get(0).getCantidad());
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si no hay suficiente stock vendible")
    void testStockInsuficienteLanzaExcepcion() {
        List<LoteDisponible> lotes = new ArrayList<>();
        lotes.add(new LoteDisponible(1, 1, "LG-2410-A", hoy.plusDays(14), 10, new BigDecimal("18.50")));
        lotes.add(new LoteDisponible(2, 1, "LG-2410-B", hoy.plusDays(20), 15, new BigDecimal("19.00")));

        // Se solicitan 30 pero solo hay 25
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> {
            fefoAllocator.asignarLotes(productoLeche, lotes, 30, hoy);
        });

        assertTrue(ex.getMessage().contains("Existencia vendible insuficiente"));
        assertTrue(ex.getMessage().contains("25"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si se solicita cantidad menor o igual a cero")
    void testCantidadInvalida() {
        List<LoteDisponible> lotes = new ArrayList<>();
        lotes.add(new LoteDisponible(1, 1, "LG-2410-A", hoy.plusDays(14), 50, new BigDecimal("18.50")));

        assertThrows(ReglaNegocioException.class, () -> {
            fefoAllocator.asignarLotes(productoLeche, lotes, 0, hoy);
        });

        assertThrows(ReglaNegocioException.class, () -> {
            fefoAllocator.asignarLotes(productoLeche, lotes, -5, hoy);
        });
    }
}
