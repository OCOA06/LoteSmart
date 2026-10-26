package com.lotesmart.modelo;

public class SesionActual {
    private static SesionActual instancia;

    private Usuario usuario;
    private Cliente cliente;

    private SesionActual() {
    }

    public static synchronized SesionActual getInstance() {
        if (instancia == null) {
            instancia = new SesionActual();
        }
        return instancia;
    }

    public synchronized void iniciarSesion(Usuario usuario, Cliente cliente) {
        this.usuario = usuario;
        this.cliente = cliente;
    }

    public synchronized void cerrarSesion() {
        this.usuario = null;
        this.cliente = null;
    }

    public synchronized boolean estaAutenticado() {
        return usuario != null;
    }

    public synchronized Usuario getUsuario() {
        return usuario;
    }

    public synchronized Cliente getCliente() {
        return cliente;
    }

    public synchronized String getUsername() {
        if (usuario != null && usuario.getUsername() != null) {
            return usuario.getUsername();
        }
        return "ANONIMO";
    }

    public synchronized RolUsuario getRol() {
        return usuario != null ? usuario.getRol() : null;
    }

    public synchronized boolean esAdmin() {
        return usuario != null && usuario.getRol() == RolUsuario.ADMIN;
    }

    public synchronized boolean esOperador() {
        return usuario != null && usuario.getRol() == RolUsuario.OPERADOR;
    }

    public synchronized boolean esCliente() {
        return usuario != null && usuario.getRol() == RolUsuario.CLIENTE;
    }
}
