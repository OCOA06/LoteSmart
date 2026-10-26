package com.lotesmart.modelo;

public enum TipoMovimiento {
    VENTA("Venta a Cliente", -1),
    MERMA("Merma / Daño", -1),
    AJUSTE_NEGATIVO("Ajuste de Inventario (-)", -1),
    AJUSTE_POSITIVO("Ajuste de Inventario (+)", 1),
    DEVOLUCION("Devolución de Venta", 1);

    private final String etiqueta;
    private final int signo;

    TipoMovimiento(String etiqueta, int signo) {
        this.etiqueta = etiqueta;
        this.signo = signo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getSigno() {
        return signo;
    }

    public static TipoMovimiento fromString(String val) {
        if (val == null) return null;
        for (TipoMovimiento t : values()) {
            if (t.name().equalsIgnoreCase(val.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de movimiento desconocido: " + val);
    }
}
