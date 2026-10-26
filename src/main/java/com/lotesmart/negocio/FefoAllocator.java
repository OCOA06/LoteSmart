package com.lotesmart.negocio;

import com.lotesmart.modelo.Asignacion;
import com.lotesmart.modelo.LoteDisponible;
import com.lotesmart.modelo.Producto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Algoritmo puro en memoria FEFO (First Expired, First Out)
 * No toca la base de datos para garantizar determinismo y facilitar pruebas unitarias.
 */
public class FefoAllocator {

    public List<Asignacion> asignarLotes(Producto producto, List<LoteDisponible> lotes, int cantidadSolicitada, LocalDate hoy)
            throws ReglaNegocioException {

        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }
        if (cantidadSolicitada <= 0) {
            throw new ReglaNegocioException("La cantidad solicitada debe ser estrictamente mayor a 0.");
        }
        if (hoy == null) {
            hoy = LocalDate.now();
        }

        // 1. Calcular fecha mínima de caducidad aceptable según política comercial del producto
        LocalDate caducidadMinima = hoy.plusDays(producto.getDiasMinimosVenta());

        if (lotes == null || lotes.isEmpty()) {
            throw new ReglaNegocioException(String.format(
                    "No hay lotes disponibles para el producto '%s' (%s). Existencia vendible: 0.",
                    producto.getNombre(), producto.getSku()));
        }

        // 2. Filtrar candidatos: existencia > 0 y fechaCaducidad >= caducidadMinima
        List<LoteDisponible> candidatos = lotes.stream()
                .filter(l -> l.getExistencia() > 0)
                .filter(l -> l.getFechaCaducidad() != null && !l.getFechaCaducidad().isBefore(caducidadMinima))
                .sorted(Comparator.comparing(LoteDisponible::getFechaCaducidad)
                        .thenComparing(LoteDisponible::getIdLote))
                .collect(Collectors.toList());

        // Calcular stock vendible total de los candidatos
        int stockVendibleTotal = candidatos.stream().mapToInt(LoteDisponible::getExistencia).sum();

        if (stockVendibleTotal < cantidadSolicitada) {
            // Contar si hay lotes descartados por política de días mínimos
            int stockExcluidoPorDias = lotes.stream()
                    .filter(l -> l.getExistencia() > 0)
                    .filter(l -> l.getFechaCaducidad() != null && l.getFechaCaducidad().isBefore(caducidadMinima))
                    .mapToInt(LoteDisponible::getExistencia)
                    .sum();

            String detalleDias = stockExcluidoPorDias > 0
                    ? String.format(" (Nota: Hay %d unidades no vendibles por requerir al menos %d días de caducidad).",
                    stockExcluidoPorDias, producto.getDiasMinimosVenta())
                    : "";

            throw new ReglaNegocioException(String.format(
                    "Existencia vendible insuficiente para '%s' (%s). Solicitada: %d, Vendible por FEFO: %d%s",
                    producto.getNombre(), producto.getSku(), cantidadSolicitada, stockVendibleTotal, detalleDias));
        }

        // 3. Repartir la cantidad solicitada entre los lotes ordenados
        List<Asignacion> asignaciones = new ArrayList<>();
        int restante = cantidadSolicitada;

        for (LoteDisponible lote : candidatos) {
            if (restante <= 0) {
                break;
            }

            int tomar = Math.min(restante, lote.getExistencia());
            Asignacion a = new Asignacion(
                    lote.getIdLote(),
                    lote.getCodigoLote(),
                    lote.getFechaCaducidad(),
                    tomar,
                    lote.getCostoUnitario(),
                    producto.getPrecioVenta()
            );
            asignaciones.add(a);
            restante -= tomar;
        }

        return asignaciones;
    }
}
