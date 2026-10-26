package com.lotesmart.vista;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.lotesmart.modelo.FiltroReporte;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.TipoMovimiento;
import com.lotesmart.reportes.ReporteExcelService;
import com.lotesmart.reportes.ReportePdfService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;

public class CentroReportesPanel extends JPanel {

    private final ReportePdfService reportePdfService;
    private final ReporteExcelService reporteExcelService;

    public CentroReportesPanel() {
        this.reportePdfService = new ReportePdfService();
        this.reporteExcelService = new ReporteExcelService();

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridLayout(3, 2, 15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        boolean esAdmin = SesionActual.getInstance().esAdmin();

        // 1. CARD: TICKET / NOTA DE VENTA FEFO (PDF)
        add(crearCardTicketVenta());

        // 2. CARD: ALERTA SANITARIA / RETIRO DE LOTE (PDF)
        add(crearCardAlertaSanitaria());

        // 3. CARD: SEMÁFORO DE CADUCIDAD Y VALOR EN RIESGO (PDF / EXCEL)
        add(crearCardSemaforo());

        // 4. CARD: REPORTE FINANCIERO DE MARGEN BRUTO (PDF / EXCEL) - SOLO ADMIN
        if (esAdmin) {
            add(crearCardFinanciero());
        } else {
            add(crearCardBloqueada("Reporte Financiero de Margen Bruto", "Requiere privilegios de Administrador para consultar utilidades y costos."));
        }

        // 5. CARD: KARDEX Y BITÁCORA DE AUDITORÍA (EXCEL) - SOLO ADMIN
        if (esAdmin) {
            add(crearCardKardexAuditoria());
        } else {
            add(crearCardBloqueada("Kardex y Bitácora de Auditoría", "Requiere privilegios de Administrador para exportar bitácora integral."));
        }
    }

    // 1. TICKET DE VENTA
    private JPanel crearCardTicketVenta() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UiUtils.aplicarEstiloCard(card);

        JLabel lblTit = new JLabel("1. Ticket / Nota de Venta FEFO (PDF)");
        lblTit.setFont(UiUtils.FONT_SUBTITULO);
        lblTit.setForeground(UiUtils.COLOR_PRIMARIO);
        card.add(lblTit, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        form.add(new JLabel("ID / Folio de Venta:"));
        JSpinner spnIdVenta = UiUtils.crearSpinnerEntero(1, 1, 1000000, 1);
        spnIdVenta.setPreferredSize(new Dimension(90, 30));
        form.add(spnIdVenta);

        JButton btnPdf = new JButton("Descargar PDF");
        btnPdf.setFont(UiUtils.FONT_BOLD);
        btnPdf.addActionListener(e -> {
            int idVenta = (int) spnIdVenta.getValue();
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Ticket_Venta_" + idVenta + ".pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reportePdfService.generarTicketVentaPdf(idVenta, fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Ticket de venta generado correctamente.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnPdf);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // 2. ALERTA SANITARIA
    private JPanel crearCardAlertaSanitaria() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UiUtils.aplicarEstiloCard(card);

        JLabel lblTit = new JLabel("2. Alerta Sanitaria de Retiro de Lote (PDF)");
        lblTit.setFont(UiUtils.FONT_SUBTITULO);
        lblTit.setForeground(new Color(180, 40, 40));
        card.add(lblTit, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        form.add(new JLabel("Código de Lote:"));
        JTextField txtLote = new JTextField("LG-2410-A", 12);
        UiUtils.aplicarFiltroMayusculas(txtLote, 30);
        form.add(txtLote);

        JButton btnPdf = new JButton("Descargar Orden Retiro (PDF)");
        btnPdf.setBackground(new Color(180, 40, 40));
        btnPdf.setForeground(Color.WHITE);
        btnPdf.addActionListener(e -> {
            String lote = txtLote.getText().trim();
            if (lote.isEmpty()) {
                UiUtils.mostrarMensajeAdvertencia(this, "Debe ingresar el código de lote.");
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Retiro_Sanitario_" + lote + ".pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reportePdfService.generarAlertaSanitariaPdf(lote, fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Orden de retiro sanitario generada correctamente.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnPdf);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // 3. SEMÁFORO DE CADUCIDAD
    private JPanel crearCardSemaforo() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UiUtils.aplicarEstiloCard(card);

        JLabel lblTit = new JLabel("3. Semáforo de Caducidad y Dinero en Riesgo");
        lblTit.setFont(UiUtils.FONT_SUBTITULO);
        lblTit.setForeground(UiUtils.COLOR_PRIMARIO);
        card.add(lblTit, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        form.add(new JLabel("Semáforo:"));
        JComboBox<String> cmbSem = new JComboBox<>(new String[]{"TODOS", "ROJO", "AMARILLO", "VERDE", "CADUCADO", "BLOQUEADO"});
        form.add(cmbSem);

        JButton btnPdf = new JButton("Exportar PDF");
        btnPdf.addActionListener(e -> {
            String f = (String) cmbSem.getSelectedItem();
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Semaforo_Caducidad_" + f + ".pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reportePdfService.generarReporteSemaforoPdf(f, fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Reporte PDF del semáforo generado con éxito.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnPdf);

        JButton btnXls = new JButton("Exportar Excel");
        btnXls.addActionListener(e -> {
            String f = (String) cmbSem.getSelectedItem();
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Semaforo_Caducidad_" + f + ".xlsx"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reporteExcelService.generarReporteSemaforoExcel(f, fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Reporte Excel del semáforo generado con éxito.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnXls);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // 4. FINANCIERO DE MARGEN BRUTO
    private JPanel crearCardFinanciero() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UiUtils.aplicarEstiloCard(card);

        JLabel lblTit = new JLabel("4. Reporte Financiero de Ventas y Margen Bruto");
        lblTit.setFont(UiUtils.FONT_SUBTITULO);
        lblTit.setForeground(UiUtils.COLOR_PRIMARIO);
        card.add(lblTit, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        form.setOpaque(false);

        DatePickerSettings ds1 = new DatePickerSettings();
        ds1.setFormatForDatesCommonEra("yyyy-MM-dd");
        DatePicker dpInicio = new DatePicker(ds1);
        dpInicio.setDate(LocalDate.now().minusDays(30));

        DatePickerSettings ds2 = new DatePickerSettings();
        ds2.setFormatForDatesCommonEra("yyyy-MM-dd");
        DatePicker dpFin = new DatePicker(ds2);
        dpFin.setDate(LocalDate.now().plusDays(1));

        form.add(new JLabel("Del:"));
        form.add(dpInicio);
        form.add(new JLabel("Al:"));
        form.add(dpFin);

        JButton btnPdf = new JButton("PDF");
        btnPdf.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Reporte_Financiero_Margen.pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reportePdfService.generarReporteFinancieroPdf(dpInicio.getDate(), dpFin.getDate(), fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Reporte Financiero PDF generado.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnPdf);

        JButton btnXls = new JButton("Excel");
        btnXls.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Reporte_Financiero_Margen.xlsx"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reporteExcelService.generarReporteFinancieroExcel(dpInicio.getDate(), dpFin.getDate(), fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Reporte Financiero Excel generado.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnXls);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // 5. KARDEX Y AUDITORÍA
    private JPanel crearCardKardexAuditoria() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UiUtils.aplicarEstiloCard(card);

        JLabel lblTit = new JLabel("5. Kardex de Movimientos y Auditoría Integral (Excel)");
        lblTit.setFont(UiUtils.FONT_SUBTITULO);
        lblTit.setForeground(UiUtils.COLOR_PRIMARIO);
        card.add(lblTit, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);

        JButton btnXls = new JButton("Exportar Libro Completo (.xlsx)");
        btnXls.setFont(UiUtils.FONT_BOLD);
        btnXls.setBackground(UiUtils.COLOR_VERDE_OK);
        btnXls.setForeground(Color.WHITE);
        btnXls.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Kardex_Auditoria_LoteSmart.xlsx"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    reporteExcelService.generarKardexYAuditoriaExcel(new FiltroReporte(), fc.getSelectedFile());
                    UiUtils.mostrarMensajeExito(this, "Libro Excel con Kardex y Bitácora de Auditoría exportado exitosamente.");
                } catch (Exception ex) {
                    UiUtils.mostrarMensajeError(this, ex.getMessage());
                }
            }
        });
        form.add(btnXls);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardBloqueada(String titulo, String mensaje) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        UiUtils.aplicarEstiloCard(card);

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(UiUtils.FONT_SUBTITULO);
        lblTit.setForeground(Color.GRAY);
        card.add(lblTit, BorderLayout.NORTH);

        JLabel lblMsg = new JLabel("<html><i>" + mensaje + "</i></html>");
        lblMsg.setForeground(new Color(120, 130, 140));
        card.add(lblMsg, BorderLayout.CENTER);

        return card;
    }
}
