package com.lotesmart.vista;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class SemaforoCellRenderer extends DefaultTableCellRenderer {

    private static final Color BG_ROJO = new Color(255, 205, 210);
    private static final Color FG_ROJO = new Color(183, 28, 28);

    private static final Color BG_AMARILLO = new Color(255, 249, 196);
    private static final Color FG_AMARILLO = new Color(245, 127, 23);

    private static final Color BG_VERDE = new Color(200, 230, 201);
    private static final Color FG_VERDE = new Color(27, 94, 32);

    private static final Color BG_CADUCADO = new Color(248, 187, 208);
    private static final Color FG_CADUCADO = new Color(136, 14, 79);

    private static final Color BG_BLOQUEADO = new Color(225, 190, 231);
    private static final Color FG_BLOQUEADO = new Color(74, 20, 140);

    private static final Color BG_AGOTADO = new Color(238, 238, 238);
    private static final Color FG_AGOTADO = new Color(117, 117, 117);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(CENTER);
        setFont(getFont().deriveFont(Font.BOLD));

        if (value == null) {
            return c;
        }

        String str = value.toString().trim().toUpperCase();
        if (!isSelected) {
            switch (str) {
                case "ROJO":
                    c.setBackground(BG_ROJO);
                    c.setForeground(FG_ROJO);
                    break;
                case "AMARILLO":
                    c.setBackground(BG_AMARILLO);
                    c.setForeground(FG_AMARILLO);
                    break;
                case "VERDE":
                    c.setBackground(BG_VERDE);
                    c.setForeground(FG_VERDE);
                    break;
                case "CADUCADO":
                    c.setBackground(BG_CADUCADO);
                    c.setForeground(FG_CADUCADO);
                    break;
                case "BLOQUEADO":
                    c.setBackground(BG_BLOQUEADO);
                    c.setForeground(FG_BLOQUEADO);
                    break;
                case "AGOTADO":
                    c.setBackground(BG_AGOTADO);
                    c.setForeground(FG_AGOTADO);
                    break;
                default:
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                    break;
            }
        }

        return c;
    }
}
