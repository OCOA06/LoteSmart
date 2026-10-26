package com.lotesmart.modelo;

public class Proveedor {
    private int idProveedor;
    private String rfc;
    private String nombre;
    private String telefono;
    private boolean activo;

    public Proveedor() {
        this.activo = true;
    }

    public Proveedor(int idProveedor, String rfc, String nombre, String telefono, boolean activo) {
        this.idProveedor = idProveedor;
        this.rfc = rfc;
        this.nombre = nombre;
        this.telefono = telefono;
        this.activo = activo;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
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
