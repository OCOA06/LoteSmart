package com.lotesmart.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {
    private int idMovimiento;
    private int idLote;
    private Integer idVenta;
    private TipoMovimiento tipo;
    private int cantidad;
    private BigDecimal precioUnitario;
    private String motivo;
    private String usuario;
    private LocalDateTime fecha;

    // Campos de visualización
    private String codigoLote;
    private String nombreProducto;
    private String skuProducto;
    private String folioVenta;

    public Movimiento() {
        this.precioUnitario = BigDecimal.ZERO;
    }

    public Movimiento(int idMovimiento, int idLote, Integer idVenta, TipoMovimiento tipo, int cantidad, BigDecimal precioUnitario, String motivo, String usuario, LocalDateTime fecha) {
        this.idMovimiento = idMovimiento;
        this.idLote = idLote;
        this.idVenta = idVenta;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        this.motivo = motivo;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdLote() {
        return idLote;
    }

    public void setIdLote(int idLote) {
        this.idLote = idLote;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getSkuProducto() {
        return skuProducto;
    }

    public void setSkuProducto(String skuProducto) {
        this.skuProducto = skuProducto;
    }

    public String getFolioVenta() {
        return folioVenta;
    }

    public void setFolioVenta(String folioVenta) {
        this.folioVenta = folioVenta;
    }

    @Override
    public String toString() {
        return tipo + " - " + cantidad + " uds (" + codigoLote + ")";
    }
}
