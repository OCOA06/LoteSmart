package com.lotesmart.vista;

import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.datos.ResultadoTabla;
import com.lotesmart.modelo.FiltroReporte;
import com.lotesmart.reportes.ReportePdfService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;

public class TrazabilidadAuditoriaPanel extends JPanel {

    private final ConsultaDAO consultaDAO;
    private final ReportePdfService reportePdfService;

    // Componentes Trazabilidad
    private JTextField txtCodigoLoteRastreo;
    private JTable tablaTrazabilidad;
    private JButton btnRastrear;
    private JButton btnExportarAlertaPdf;
    private String loteConsultado = "";

    // Componentes Auditoría
    private JTable tablaAuditoria;
    private JComboBox<String> cmbFiltroTablaAuditoria;
    private JButton btnActualizarAuditoria;

    public TrazabilidadAuditoriaPanel() {
        this.consultaDAO = new ConsultaDAO();
        this.reportePdfService = new ReportePdfService();

        initComponents();
        cargarAuditoria();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Trazabilidad y Retiro Sanitario de Mercado", crearPanelTrazabilidad());
        tabs.addTab("Bitácora Inmutable de Auditoría", crearPanelAuditoria());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel crearPanelTrazabilidad() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        // Barra superior de búsqueda
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.add(new JLabel("Código de Lote a Investigar:"));
        txtCodigoLoteRastreo = new JTextField(15);
        UiUtils.aplicarFiltroMayusculas(txtCodigoLoteRastreo, 30);
        top.add(txtCodigoLoteRastreo);

        btnRastrear = new JButton("Rastrear Despachos");
        btnRastrear.setFont(UiUtils.FONT_BOLD);
        btnRastrear.setBackground(UiUtils.COLOR_PRIMARIO);
        btnRastrear.setForeground(Color.WHITE);
        btnRastrear.addActionListener(e -> ejecutarRastreoLote());
        top.add(btnRastrear);

        btnExportarAlertaPdf = new JButton("Descargar Orden de Retiro Sanitario (PDF)");
        btnExportarAlertaPdf.setBackground(new Color(180, 40, 40));
        btnExportarAlertaPdf.setForeground(Color.WHITE);
        btnExportarAlertaPdf.setEnabled(false);
        btnExportarAlertaPdf.addActionListener(e -> exportarOrdenRetiroPdf());
        top.add(btnExportarAlertaPdf);

        p.add(top, BorderLayout.NORTH);

        tablaTrazabilidad = new JTable();
        UiUtils.darFormatoTabla(tablaTrazabilidad);
        JScrollPane scroll = new JScrollPane(tablaTrazabilidad);
        scroll.setBorder(BorderFactory.createTitledBorder("Clientes y Ventas que recibieron unidades de este lote"));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel crearPanelAuditoria() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.add(new JLabel("Filtrar por Entidad / Tabla:"));
        cmbFiltroTablaAuditoria = new JComboBox<>(new String[]{"TODAS", "producto", "lote", "venta", "cliente", "proveedor", "usuario"});
        cmbFiltroTablaAuditoria.addActionListener(e -> cargarAuditoria());
        top.add(cmbFiltroTablaAuditoria);

        btnActualizarAuditoria = new JButton("Actualizar Bitácora");
        btnActualizarAuditoria.addActionListener(e -> cargarAuditoria());
        top.add(btnActualizarAuditoria);

        p.add(top, BorderLayout.NORTH);

        tablaAuditoria = new JTable();
        UiUtils.darFormatoTabla(tablaAuditoria);
        JScrollPane scroll = new JScrollPane(tablaAuditoria);
        scroll.setBorder(BorderFactory.createTitledBorder("Historial de Transacciones Registradas por Triggers de Base de Datos"));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private void ejecutarRastreoLote() {
        String codigo = txtCodigoLoteRastreo.getText().trim();
        if (codigo.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "Debe ingresar el código de lote a rastrear.");
            return;
        }

        loteConsultado = codigo;
        btnRastrear.setEnabled(false);
        new SwingWorker<ResultadoTabla, Void>() {
            private Exception error;

            @Override
            protected ResultadoTabla doInBackground() {
                try {
                    return consultaDAO.obtenerTrazabilidad(codigo);
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                btnRastrear.setEnabled(true);
                if (error != null) {
                    UiUtils.mostrarMensajeError(TrazabilidadAuditoriaPanel.this, "Error al rastrear: " + error.getMessage());
                    btnExportarAlertaPdf.setEnabled(false);
                    return;
                }
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaTrazabilidad.setModel(res.aModeloTablaPoblado());
                        btnExportarAlertaPdf.setEnabled(res.getCantidadFilas() > 0);
                        if (res.getCantidadFilas() == 0) {
                            UiUtils.mostrarMensajeAdvertencia(TrazabilidadAuditoriaPanel.this,
                                    "No se encontraron ventas registradas para el lote '" + codigo + "'.\n" +
                                    "Verifique el código o consulte si las existencias permanecen íntegras en almacén.");
                        }
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }

    private void exportarOrdenRetiroPdf() {
        if (loteConsultado.isEmpty()) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Alerta_Sanitaria_Retiro_" + loteConsultado + ".pdf"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File destino = fc.getSelectedFile();
            try {
                reportePdfService.generarAlertaSanitariaPdf(loteConsultado, destino);
                UiUtils.mostrarMensajeExito(this, "Orden de Retiro Sanitario generada exitosamente en:\n" + destino.getAbsolutePath());
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, "Error al generar documento PDF: " + e.getMessage());
            }
        }
    }

    public void cargarAuditoria() {
        String tabla = (String) cmbFiltroTablaAuditoria.getSelectedItem();
        new SwingWorker<ResultadoTabla, Void>() {
            @Override
            protected ResultadoTabla doInBackground() throws Exception {
                FiltroReporte f = new FiltroReporte();
                f.setTabla(tabla);
                return consultaDAO.obtenerAuditoria(f);
            }

            @Override
            protected void done() {
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaAuditoria.setModel(res.aModeloTablaPoblado());
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }
}
