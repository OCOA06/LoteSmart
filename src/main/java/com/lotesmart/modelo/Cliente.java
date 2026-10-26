package com.lotesmart.modelo;

public class Cliente {
    private int idCliente;
    private Integer idUsuario;
    private String rfc;
    private String nombre;
    private String telefono;
    private String correo;
    private boolean activo;

    public Cliente() {
        this.activo = true;
    }

    public Cliente(int idCliente, Integer idUsuario, String rfc, String nombre, String telefono, String correo, boolean activo) {
        this.idCliente = idCliente;
        this.idUsuario = idUsuario;
        this.rfc = rfc;
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.activo = activo;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre + (rfc != null && !rfc.isEmpty() ? " [" + rfc + "]" : "");
    }
}
