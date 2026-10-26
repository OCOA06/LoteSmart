package com.lotesmart.modelo;

import java.time.LocalDateTime;

public class Usuario {
    private int idUsuario;
    private String username;
    private String passwordHash;
    private String nombreCompleto;
    private RolUsuario rol;
    private boolean activo;
    private LocalDateTime fechaCreacion;

    public Usuario() {
        this.activo = true;
    }

    public Usuario(int idUsuario, String username, String passwordHash, String nombreCompleto, RolUsuario rol, boolean activo, LocalDateTime fechaCreacion) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return nombreCompleto + " (" + username + " - " + (rol != null ? rol.getDescripcion() : "") + ")";
    }
}
