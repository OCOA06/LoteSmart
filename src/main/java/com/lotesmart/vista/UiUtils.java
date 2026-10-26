package com.lotesmart.vista;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class UiUtils {

    public static final Color COLOR_PRIMARIO = new Color(24, 76, 120);
    public static final Color COLOR_SECUNDARIO = new Color(70, 95, 120);
    public static final Color COLOR_FONDO_CARD = new Color(248, 250, 252);
    public static final Color COLOR_BORDE = new Color(220, 226, 235);
    public static final Color COLOR_VERDE_OK = new Color(40, 167, 69);
    public static final Color COLOR_ROJO_ERROR = new Color(220, 53, 69);

    public static final Font FONT_TITULO = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITULO = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);

    private static final DecimalFormat FORMATO_MONEDA = new DecimalFormat("$#,##0.00");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void inicializarTema() {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            // fallback a LookAndFeel del sistema
        }
    }

    public static void aplicarEstiloCard(JComponent comp) {
        comp.setBackground(COLOR_FONDO_CARD);
        comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDE, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        comp.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
    }

    public static void aplicarFiltroMayusculas(JTextField textField, int maxLength) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String upper = string.toUpperCase().replaceAll("\\s+", "");
                if (maxLength <= 0 || (fb.getDocument().getLength() + upper.length() <= maxLength)) {
                    super.insertString(fb, offset, upper, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                String upper = text.toUpperCase().replaceAll("\\s+", "");
                if (maxLength <= 0 || (fb.getDocument().getLength() - length + upper.length() <= maxLength)) {
                    super.replace(fb, offset, length, upper, attrs);
                }
            }
        });
    }

    public static void aplicarValidacionRfcVisual(JTextField textField) {
        aplicarFiltroMayusculas(textField, 13);
        final Border defaultBorder = textField.getBorder();
        final Pattern rfcPattern = Pattern.compile("^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$");

        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void validar() {
                String texto = textField.getText().trim();
                if (texto.isEmpty()) {
                    textField.setBorder(defaultBorder);
                } else if (rfcPattern.matcher(texto).matches()) {
                    textField.setBorder(BorderFactory.createLineBorder(COLOR_VERDE_OK, 2));
                } else {
                    textField.setBorder(BorderFactory.createLineBorder(COLOR_ROJO_ERROR, 2));
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validar(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validar(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validar(); }
        });
    }

    public static JSpinner crearSpinnerEntero(int valorInicial, int min, int max, int paso) {
        SpinnerNumberModel model = new SpinnerNumberModel(valorInicial, min, max, paso);
        JSpinner spinner = new JSpinner(model);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setEditable(true);
        }
        return spinner;
    }

    public static JSpinner crearSpinnerDecimal(double valorInicial, double min, double max, double paso) {
        SpinnerNumberModel model = new SpinnerNumberModel(valorInicial, min, max, paso);
        JSpinner spinner = new JSpinner(model);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.NumberEditor) {
            ((JSpinner.NumberEditor) editor).getFormat().applyPattern("#,##0.00");
        }
        return spinner;
    }

    public static void darFormatoTabla(JTable tabla) {
        tabla.setRowHeight(26);
        tabla.setFont(FONT_REGULAR);
        tabla.getTableHeader().setFont(FONT_BOLD);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(235, 238, 242));
    }

    public static String formatoMoneda(BigDecimal valor) {
        if (valor == null) return "$0.00";
        return FORMATO_MONEDA.format(valor);
    }

    public static String formatoFecha(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.format(FORMATO_FECHA);
    }

    public static void mostrarMensajeExito(Component parent, String mensaje) {
        JOptionPane.showMessageDialog(parent, mensaje, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void mostrarMensajeError(Component parent, String mensaje) {
        JOptionPane.showMessageDialog(parent, mensaje, "Error en Operación", JOptionPane.ERROR_MESSAGE);
    }

    public static void mostrarMensajeAdvertencia(Component parent, String mensaje) {
        JOptionPane.showMessageDialog(parent, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirmar(Component parent, String mensaje, String titulo) {
        int resp = JOptionPane.showConfirmDialog(parent, mensaje, titulo, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return resp == JOptionPane.YES_OPTION;
    }
}
