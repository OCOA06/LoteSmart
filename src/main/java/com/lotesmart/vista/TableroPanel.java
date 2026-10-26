package com.lotesmart.vista;

import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.datos.ErroresBD;
import com.lotesmart.datos.ResultadoTabla;
import com.lotesmart.modelo.FiltroReporte;
import com.lotesmart.modelo.ResumenTablero;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class TableroPanel extends JPanel {

    private final ConsultaDAO consultaDAO;

    // KPI Labels
    private JLabel lblValorRiesgo;
    private JLabel lblValorNoVendible;
    private JLabel lblLotesCaducados;
    private JLabel lblAlertasReorden;
    private JLabel lblLotesVigentes;
    private JLabel lblVentasHoy;

    // Tablas
    private JTable tablaSemaforo;
    private JTable tablaReorden;
    private JComboBox<String> cmbFiltroSemaforo;
    private JButton btnActualizar;

    public TableroPanel() {
        this.consultaDAO = new ConsultaDAO();
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Barra superior
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblTitulo = new JLabel("Tablero de Control - Monitoreo Sanitario y Financiero");
        lblTitulo.setFont(UiUtils.FONT_TITULO);
        lblTitulo.setForeground(UiUtils.COLOR_PRIMARIO);

        JPanel pAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        cmbFiltroSemaforo = new JComboBox<>(new String[]{"TODOS", "ROJO", "AMARILLO", "VERDE", "CADUCADO", "BLOQUEADO"});
        cmbFiltroSemaforo.addActionListener(e -> cargarTablaSemaforo());

        btnActualizar = new JButton("Actualizar Datos");
        btnActualizar.setFont(UiUtils.FONT_BOLD);
        btnActualizar.addActionListener(e -> cargarDatos());

        pAcciones.add(new JLabel("Filtro Semáforo:"));
        pAcciones.add(cmbFiltroSemaforo);
        pAcciones.add(btnActualizar);

        topPanel.add(lblTitulo, BorderLayout.WEST);
        topPanel.add(pAcciones, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Panel central con KPIs y Tablas
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Grid de 6 KPIs
        JPanel kpiGrid = new JPanel(new GridLayout(1, 6, 10, 10));
        kpiGrid.setPreferredSize(new Dimension(100, 95));

        lblValorRiesgo = new JLabel("$0.00", JLabel.CENTER);
        lblValorNoVendible = new JLabel("$0.00", JLabel.CENTER);
        lblLotesCaducados = new JLabel("0", JLabel.CENTER);
        lblAlertasReorden = new JLabel("0", JLabel.CENTER);
        lblLotesVigentes = new JLabel("0", JLabel.CENTER);
        lblVentasHoy = new JLabel("$0.00", JLabel.CENTER);

        kpiGrid.add(crearCardKpi("Dinero en Riesgo (<60d)", lblValorRiesgo, new Color(255, 235, 238), new Color(183, 28, 28)));
        kpiGrid.add(crearCardKpi("Valor No Vendible", lblValorNoVendible, new Color(243, 229, 245), new Color(74, 20, 140)));
        kpiGrid.add(crearCardKpi("Lotes Caducados", lblLotesCaducados, new Color(255, 205, 210), new Color(198, 40, 40)));
        kpiGrid.add(crearCardKpi("Alertas de Reorden", lblAlertasReorden, new Color(255, 243, 224), new Color(230, 81, 0)));
        kpiGrid.add(crearCardKpi("Lotes Vigentes", lblLotesVigentes, new Color(232, 245, 233), new Color(27, 94, 32)));
        kpiGrid.add(crearCardKpi("Ventas del Día", lblVentasHoy, new Color(227, 242, 253), new Color(13, 71, 161)));

        centerPanel.add(kpiGrid, BorderLayout.NORTH);

        // Tablas divididas por SplitPane
        tablaSemaforo = new JTable();
        UiUtils.darFormatoTabla(tablaSemaforo);
        JScrollPane scrollSemaforo = new JScrollPane(tablaSemaforo);
        scrollSemaforo.setBorder(BorderFactory.createTitledBorder("Semáforo de Caducidad de Lotes (Prioridad FEFO)"));

        tablaReorden = new JTable();
        UiUtils.darFormatoTabla(tablaReorden);
        JScrollPane scrollReorden = new JScrollPane(tablaReorden);
        scrollReorden.setBorder(BorderFactory.createTitledBorder("Alertas de Reorden y Existencias Generales"));

        JSplitPane splitTablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollSemaforo, scrollReorden);
        splitTablas.setResizeWeight(0.65);
        centerPanel.add(splitTablas, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel crearCardKpi(String titulo, JLabel lblValor, Color fondo, Color colorTexto) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(fondo);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel lblTit = new JLabel(titulo, JLabel.CENTER);
        lblTit.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        lblTit.setForeground(new Color(80, 90, 100));

        lblValor.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        lblValor.setForeground(colorTexto);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    public void cargarDatos() {
        btnActualizar.setEnabled(false);
        new SwingWorker<ResumenTablero, Void>() {
            private ResultadoTabla resStock;
            private Exception error;

            @Override
            protected ResumenTablero doInBackground() {
                try {
                    consultaDAO.ejecutarSpMarcarCaducados();
                    ResumenTablero r = consultaDAO.obtenerResumenTablero();
                    resStock = consultaDAO.obtenerStockProductos();
                    return r;
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                btnActualizar.setEnabled(true);
                if (error != null) {
                    UiUtils.mostrarMensajeError(TableroPanel.this, "No se pudo actualizar el Tablero: " + ErroresBD.traducir(error));
                    return;
                }
                try {
                    ResumenTablero r = get();
                    if (r != null) {
                        lblValorRiesgo.setText(UiUtils.formatoMoneda(r.getValorEnRiesgo()));
                        lblValorNoVendible.setText(UiUtils.formatoMoneda(r.getValorNoVendible()));
                        lblLotesCaducados.setText(String.valueOf(r.getLotesCaducados()));
                        lblAlertasReorden.setText(String.valueOf(r.getAlertasReorden()));
                        lblLotesVigentes.setText(String.valueOf(r.getTotalLotesVigentes()));
                        lblVentasHoy.setText(UiUtils.formatoMoneda(r.getTotalVentasHoy()));
                    }
                    if (resStock != null) {
                        tablaReorden.setModel(resStock.aModeloTablaPoblado());
                    }
                    cargarTablaSemaforo();
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }

    private void cargarTablaSemaforo() {
        String filtro = (String) cmbFiltroSemaforo.getSelectedItem();
        new SwingWorker<ResultadoTabla, Void>() {
            private Exception error;

            @Override
            protected ResultadoTabla doInBackground() {
                try {
                    FiltroReporte f = new FiltroReporte();
                    f.setSemaforo(filtro);
                    return consultaDAO.obtenerLotesDetalle(f);
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    return;
                }
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaSemaforo.setModel(res.aModeloTablaPoblado());
                        // Aplicar renderizador de semáforo a la columna correspondiente
                        int colSemaforo = -1;
                        for (int i = 0; i < tablaSemaforo.getColumnCount(); i++) {
                            if (tablaSemaforo.getColumnName(i).equalsIgnoreCase("Semáforo")) {
                                colSemaforo = i;
                                break;
                            }
                        }
                        if (colSemaforo >= 0) {
                            tablaSemaforo.getColumnModel().getColumn(colSemaforo).setCellRenderer(new SemaforoCellRenderer());
                        }
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }
}
