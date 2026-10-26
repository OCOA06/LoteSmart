package com.lotesmart.modelo;

public enum EstadoVenta {
    REGISTRADA("Registrada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoVenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoVenta fromString(String val) {
        if (val == null) return null;
        for (EstadoVenta e : values()) {
            if (e.name().equalsIgnoreCase(val.trim())) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de venta desconocido: " + val);
    }
}
