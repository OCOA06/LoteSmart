package com.lotesmart.modelo;

public enum RolUsuario {
    ADMIN("Administrador"),
    OPERADOR("Operador de Almacén"),
    CLIENTE("Cliente B2B");

    private final String descripcion;

    RolUsuario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static RolUsuario fromString(String rol) {
        if (rol == null) return null;
        for (RolUsuario r : values()) {
            if (r.name().equalsIgnoreCase(rol.trim())) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rol de usuario desconocido: " + rol);
    }
}
