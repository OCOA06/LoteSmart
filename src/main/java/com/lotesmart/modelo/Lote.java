package com.lotesmart.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Lote {
    private int idLote;
    private int idProducto;
    private int idProveedor;
    private String codigoLote;
    private LocalDate fechaCaducidad;
    private int cantidadInicial;
    private int cantidadDisponible;
    private BigDecimal costoUnitario;
    private EstadoLote estado;
    private String motivoBloqueo;

    // Campos enriquecidos de consulta / vistas
    private String nombreProducto;
    private String skuProducto;
    private String unidadProducto;
    private int diasMinimosVenta;
    private String nombreProveedor;
    private int diasRestantes;
    private String semaforo;
    private BigDecimal valorExistencia;

    public Lote() {
        this.estado = EstadoLote.VIGENTE;
        this.costoUnitario = BigDecimal.ZERO;
    }

    public Lote(int idLote, int idProducto, int idProveedor, String codigoLote, LocalDate fechaCaducidad, int cantidadInicial, int cantidadDisponible, BigDecimal costoUnitario, EstadoLote estado, String motivoBloqueo) {
        this.idLote = idLote;
        this.idProducto = idProducto;
        this.idProveedor = idProveedor;
        this.codigoLote = codigoLote;
        this.fechaCaducidad = fechaCaducidad;
        this.cantidadInicial = cantidadInicial;
        this.cantidadDisponible = cantidadDisponible;
        this.costoUnitario = costoUnitario;
        this.estado = estado;
        this.motivoBloqueo = motivoBloqueo;
    }

    public int getIdLote() {
        return idLote;
    }

    public void setIdLote(int idLote) {
        this.idLote = idLote;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
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

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(int cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public EstadoLote getEstado() {
        return estado;
    }

    public void setEstado(EstadoLote estado) {
        this.estado = estado;
    }

    public String getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public void setMotivoBloqueo(String motivoBloqueo) {
        this.motivoBloqueo = motivoBloqueo;
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

    public String getUnidadProducto() {
        return unidadProducto;
    }

    public void setUnidadProducto(String unidadProducto) {
        this.unidadProducto = unidadProducto;
    }

    public int getDiasMinimosVenta() {
        return diasMinimosVenta;
    }

    public void setDiasMinimosVenta(int diasMinimosVenta) {
        this.diasMinimosVenta = diasMinimosVenta;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public int getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(int diasRestantes) {
        this.diasRestantes = diasRestantes;
    }

    public String getSemaforo() {
        return semaforo;
    }

    public void setSemaforo(String semaforo) {
        this.semaforo = semaforo;
    }

    public BigDecimal getValorExistencia() {
        return valorExistencia;
    }

    public void setValorExistencia(BigDecimal valorExistencia) {
        this.valorExistencia = valorExistencia;
    }

    @Override
    public String toString() {
        return codigoLote + " (Caduca: " + fechaCaducidad + " | Disp: " + cantidadDisponible + ")";
    }
}
