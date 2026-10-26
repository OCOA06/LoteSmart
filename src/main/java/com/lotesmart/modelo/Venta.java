package com.lotesmart.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venta {
    private int idVenta;
    private LocalDateTime fecha;
    private int idCliente;
    private String cliente;
    private Integer idUsuario;
    private String username;
    private BigDecimal total;
    private EstadoVenta estado;
    private String motivoCancelacion;
    private LocalDateTime fechaCancelacion;

    // Campos calculados / de consulta
    private String folio;
    private BigDecimal costoTotal;
    private BigDecimal margenBruto;
    private List<LineaVenta> lineas;

    public Venta() {
        this.estado = EstadoVenta.REGISTRADA;
        this.total = BigDecimal.ZERO;
        this.costoTotal = BigDecimal.ZERO;
        this.margenBruto = BigDecimal.ZERO;
        this.lineas = new ArrayList<>();
    }

    public Venta(int idVenta, LocalDateTime fecha, int idCliente, String cliente, Integer idUsuario, BigDecimal total, EstadoVenta estado) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.idCliente = idCliente;
        this.cliente = cliente;
        this.idUsuario = idUsuario;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.estado = estado != null ? estado : EstadoVenta.REGISTRADA;
        this.lineas = new ArrayList<>();
        this.costoTotal = BigDecimal.ZERO;
        this.margenBruto = BigDecimal.ZERO;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getFolio() {
        if (folio != null && !folio.isEmpty()) return folio;
        if (idVenta > 0) return String.format("V-%06d", idVenta);
        return "V-NUEVA";
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(BigDecimal costoTotal) {
        this.costoTotal = costoTotal;
    }

    public BigDecimal getMargenBruto() {
        return margenBruto;
    }

    public void setMargenBruto(BigDecimal margenBruto) {
        this.margenBruto = margenBruto;
    }

    public List<LineaVenta> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaVenta> lineas) {
        this.lineas = lineas != null ? lineas : new ArrayList<>();
    }

    @Override
    public String toString() {
        return getFolio() + " - " + cliente + " ($" + total + ")";
    }
}
