package com.lotesmart.modelo;

public enum EstadoLote {
    VIGENTE("Vigente"),
    AGOTADO("Agotado"),
    CADUCADO("Caducado"),
    BLOQUEADO("Bloqueado");

    private final String etiqueta;

    EstadoLote(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoLote fromString(String val) {
        if (val == null) return null;
        for (EstadoLote e : values()) {
            if (e.name().equalsIgnoreCase(val.trim())) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de lote desconocido: " + val);
    }
}
