package com.lotesmart.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoteDisponible {
    private int idLote;
    private int idProducto;
    private String codigoLote;
    private LocalDate fechaCaducidad;
    private int existencia;
    private BigDecimal costoUnitario;

    public LoteDisponible() {
    }

    public LoteDisponible(int idLote, int idProducto, String codigoLote, LocalDate fechaCaducidad, int existencia, BigDecimal costoUnitario) {
        this.idLote = idLote;
        this.idProducto = idProducto;
        this.codigoLote = codigoLote;
        this.fechaCaducidad = fechaCaducidad;
        this.existencia = existencia;
        this.costoUnitario = costoUnitario;
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

    public int getExistencia() {
        return existencia;
    }

    public void setExistencia(int existencia) {
        this.existencia = existencia;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    @Override
    public String toString() {
        return codigoLote + " [ID: " + idLote + ", Cad: " + fechaCaducidad + ", Exist: " + existencia + "]";
    }
}
