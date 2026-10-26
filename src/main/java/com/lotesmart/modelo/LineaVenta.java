package com.lotesmart.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LineaVenta {
    private Producto producto;
    private int cantidad;
    private BigDecimal precioUnitario;
    private List<Asignacion> asignaciones;

    public LineaVenta() {
        this.asignaciones = new ArrayList<>();
        this.precioUnitario = BigDecimal.ZERO;
    }

    public LineaVenta(Producto producto, int cantidad, BigDecimal precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        this.asignaciones = new ArrayList<>();
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        if (precioUnitario == null) return BigDecimal.ZERO;
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public List<Asignacion> getAsignaciones() {
        return asignaciones;
    }

    public void setAsignaciones(List<Asignacion> asignaciones) {
        this.asignaciones = asignaciones != null ? asignaciones : new ArrayList<>();
    }

    public String getResumenLotes() {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return "Pendiente de simular / asignar";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < asignaciones.size(); i++) {
            if (i > 0) sb.append(", ");
            Asignacion a = asignaciones.get(i);
            sb.append(a.getCodigoLote()).append(" (").append(a.getCantidad()).append(" uds, ").append(a.getFechaCaducidad()).append(")");
        }
        return sb.toString();
    }
}
