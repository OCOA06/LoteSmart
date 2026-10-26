package com.lotesmart.modelo;

import java.math.BigDecimal;

public class Producto {
    private int idProducto;
    private String sku;
    private String nombre;
    private String unidad;
    private BigDecimal precioVenta;
    private int stockMinimo;
    private int diasMinimosVenta;
    private boolean activo;

    // Campos de conveniencia para vistas/reportes
    private int stockVigente;
    private int stockNoVendible;
    private int stockTotal;
    private boolean alertaReorden;

    public Producto() {
        this.precioVenta = BigDecimal.ZERO;
        this.stockMinimo = 0;
        this.diasMinimosVenta = 0;
        this.activo = true;
    }

    public Producto(int idProducto, String sku, String nombre, String unidad, BigDecimal precioVenta, int stockMinimo, int diasMinimosVenta, boolean activo) {
        this.idProducto = idProducto;
        this.sku = sku;
        this.nombre = nombre;
        this.unidad = unidad;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
        this.diasMinimosVenta = diasMinimosVenta;
        this.activo = activo;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public int getDiasMinimosVenta() {
        return diasMinimosVenta;
    }

    public void setDiasMinimosVenta(int diasMinimosVenta) {
        this.diasMinimosVenta = diasMinimosVenta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getStockVigente() {
        return stockVigente;
    }

    public void setStockVigente(int stockVigente) {
        this.stockVigente = stockVigente;
    }

    public int getStockNoVendible() {
        return stockNoVendible;
    }

    public void setStockNoVendible(int stockNoVendible) {
        this.stockNoVendible = stockNoVendible;
    }

    public int getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(int stockTotal) {
        this.stockTotal = stockTotal;
    }

    public boolean isAlertaReorden() {
        return alertaReorden;
    }

    public void setAlertaReorden(boolean alertaReorden) {
        this.alertaReorden = alertaReorden;
    }

    @Override
    public String toString() {
        return nombre + " [" + sku + "] - $" + (precioVenta != null ? precioVenta.toString() : "0.00");
    }
}
