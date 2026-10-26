package com.lotesmart.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Asignacion {
    private int idLote;
    private String codigoLote;
    private LocalDate fechaCaducidad;
    private int cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal precioUnitario;

    public Asignacion() {
        this.costoUnitario = BigDecimal.ZERO;
        this.precioUnitario = BigDecimal.ZERO;
    }

    public Asignacion(int idLote, String codigoLote, LocalDate fechaCaducidad, int cantidad, BigDecimal costoUnitario, BigDecimal precioUnitario) {
        this.idLote = idLote;
        this.codigoLote = codigoLote;
        this.fechaCaducidad = fechaCaducidad;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario != null ? costoUnitario : BigDecimal.ZERO;
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }

    public int getIdLote() {
        return idLote;
    }

    public void setIdLote(int idLote) {
        this.idLote = idLote;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public LocalDate getFechaCaducidad() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(LocalDate fechaCaducidad) {
        this.fechaCaducidad = fechaCaducidad;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return precioUnitario != null ? precioUnitario.multiply(BigDecimal.valueOf(cantidad)) : BigDecimal.ZERO;
    }

    public BigDecimal getCostoTotal() {
        return costoUnitario != null ? costoUnitario.multiply(BigDecimal.valueOf(cantidad)) : BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return codigoLote + " (Cad: " + fechaCaducidad + ") x " + cantidad + " uds";
    }
}
