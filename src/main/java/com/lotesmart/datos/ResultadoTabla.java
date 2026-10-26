package com.lotesmart.datos;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class ResultadoTabla {
    private final String[] columnas;
    private final List<Object[]> filas;

    public ResultadoTabla(String[] columnas) {
        this.columnas = columnas != null ? columnas : new String[0];
        this.filas = new ArrayList<>();
    }

    public ResultadoTabla(String[] columnas, List<Object[]> filas) {
        this.columnas = columnas != null ? columnas : new String[0];
        this.filas = filas != null ? filas : new ArrayList<>();
    }

    public void agregarFila(Object[] fila) {
        if (fila != null) {
            this.filas.add(fila);
        }
    }

    public String[] getColumnas() {
        return columnas;
    }

    public List<Object[]> getFilas() {
        return filas;
    }

    public int getCantidadFilas() {
        return filas.size();
    }

    public int getCantidadColumnas() {
        return columnas.length;
    }

    public Object getValor(int fila, int columna) {
        if (fila >= 0 && fila < filas.size()) {
            Object[] f = filas.get(fila);
            if (columna >= 0 && columna < f.length) {
                return f[columna];
            }
        }
        return null;
    }

    public DefaultTableModel aModeloTabla() {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Principio Poka-Yoke: Cero edición en celda de JTable
            }
        };
    }

    public DefaultTableModel aModeloTablaPoblado() {
        DefaultTableModel model = aModeloTabla();
        for (Object[] fila : filas) {
            model.addRow(fila);
        }
        return model;
    }
}
