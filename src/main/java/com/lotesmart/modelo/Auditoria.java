package com.lotesmart.modelo;

import java.time.LocalDateTime;

public class Auditoria {
    private int idAuditoria;
    private String tabla;
    private int idRegistro;
    private String accion;
    private String detalle;
    private String usuario;
    private LocalDateTime fecha;

    public Auditoria() {
    }

    public Auditoria(int idAuditoria, String tabla, int idRegistro, String accion, String detalle, String usuario, LocalDateTime fecha) {
        this.idAuditoria = idAuditoria;
        this.tabla = tabla;
        this.idRegistro = idRegistro;
        this.accion = accion;
        this.detalle = detalle;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public int getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(int idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public String getTabla() {
        return tabla;
    }

    public void setTabla(String tabla) {
        this.tabla = tabla;
    }

    public int getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(int idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
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

    @Override
    public String toString() {
        return "[" + fecha + "] " + tabla + " #" + idRegistro + " - " + accion + " (" + usuario + ")";
    }
}
