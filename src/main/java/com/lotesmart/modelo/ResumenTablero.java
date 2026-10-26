package com.lotesmart.modelo;

import java.math.BigDecimal;

public class ResumenTablero {
    private BigDecimal valorEnRiesgo;
    private BigDecimal valorNoVendible;
    private int lotesCaducados;
    private int alertasReorden;
    private int totalLotesVigentes;
    private BigDecimal totalVentasHoy;

    public ResumenTablero() {
        this.valorEnRiesgo = BigDecimal.ZERO;
        this.valorNoVendible = BigDecimal.ZERO;
        this.lotesCaducados = 0;
        this.alertasReorden = 0;
        this.totalLotesVigentes = 0;
        this.totalVentasHoy = BigDecimal.ZERO;
    }

    public BigDecimal getValorEnRiesgo() {
        return valorEnRiesgo;
    }

    public void setValorEnRiesgo(BigDecimal valorEnRiesgo) {
        this.valorEnRiesgo = valorEnRiesgo != null ? valorEnRiesgo : BigDecimal.ZERO;
    }

    public BigDecimal getValorNoVendible() {
        return valorNoVendible;
    }

    public void setValorNoVendible(BigDecimal valorNoVendible) {
        this.valorNoVendible = valorNoVendible != null ? valorNoVendible : BigDecimal.ZERO;
    }

    public int getLotesCaducados() {
        return lotesCaducados;
    }

    public void setLotesCaducados(int lotesCaducados) {
        this.lotesCaducados = lotesCaducados;
    }

    public int getAlertasReorden() {
        return alertasReorden;
    }

    public void setAlertasReorden(int alertasReorden) {
        this.alertasReorden = alertasReorden;
    }

    public int getTotalLotesVigentes() {
        return totalLotesVigentes;
    }

    public void setTotalLotesVigentes(int totalLotesVigentes) {
        this.totalLotesVigentes = totalLotesVigentes;
    }

    public BigDecimal getTotalVentasHoy() {
        return totalVentasHoy;
    }

    public void setTotalVentasHoy(BigDecimal totalVentasHoy) {
        this.totalVentasHoy = totalVentasHoy != null ? totalVentasHoy : BigDecimal.ZERO;
    }
}
