package com.lotesmart.vista;

import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.datos.ProductoDAO;
import com.lotesmart.datos.ResultadoTabla;
import com.lotesmart.modelo.Cliente;
import com.lotesmart.modelo.LineaVenta;
import com.lotesmart.modelo.Producto;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.negocio.VentaService;
import com.lotesmart.reportes.ReportePdfService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PortalClientePanel extends JPanel {

    private final VentaService ventaService;
    private final ProductoDAO productoDAO;
    private final ConsultaDAO consultaDAO;
    private final ReportePdfService reportePdfService;

    // Pedido
    private JTable tablaProductosCatalogo;
    private JSpinner spnCantidadPedido;
    private JButton btnAgregarAlPedido;
    private JTable tablaCarritoCliente;
    private DefaultTableModel modeloCarritoCliente;
    private List<LineaVenta> carritoCliente;
    private JLabel lblTotalPedido;
    private JButton btnSimularFefo;
    private JButton btnConfirmarPedido;

    // Mis Compras
    private JTable tablaMisCompras;
    private JButton btnDescargarTicketCertificado;

    public PortalClientePanel() {
        this.ventaService = new VentaService();
        this.productoDAO = new ProductoDAO();
        this.consultaDAO = new ConsultaDAO();
        this.reportePdfService = new ReportePdfService();
        this.carritoCliente = new ArrayList<>();

        initComponents();
        cargarCatalogoProductos();
        cargarMisCompras();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Cliente cli = SesionActual.getInstance().getCliente();
        String nomCliente = cli != null ? cli.getNombre() : "Cliente B2B";
        String rfcCliente = cli != null ? cli.getRfc() : "";

        // Banner de bienvenida
        JPanel banner = new JPanel(new BorderLayout());
        UiUtils.aplicarEstiloCard(banner);
        JLabel lblBienvenido = new JLabel("Portal B2B de Autopedido y Trazabilidad - " + nomCliente);
        lblBienvenido.setFont(UiUtils.FONT_TITULO);
        lblBienvenido.setForeground(UiUtils.COLOR_PRIMARIO);

        JLabel lblRfc = new JLabel("RFC Vinculado: " + rfcCliente + " (Despacho regulado bajo norma FEFO)");
        lblRfc.setFont(UiUtils.FONT_REGULAR);

        banner.add(lblBienvenido, BorderLayout.NORTH);
        banner.add(lblRfc, BorderLayout.SOUTH);
        add(banner, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catálogo y Autopedido FEFO", crearTabAutopedido());
        tabs.addTab("Mis Compras y Certificados de Lote", crearTabMisCompras());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel crearTabAutopedido() {
        JPanel p = new JPanel(new GridLayout(2, 1, 10, 10));

        // Sub-panel superior: Catálogo con existencias vendibles
        JPanel pTop = new JPanel(new BorderLayout(5, 5));
        tablaProductosCatalogo = new JTable();
        tablaProductosCatalogo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaProductosCatalogo);
        JScrollPane scrollCat = new JScrollPane(tablaProductosCatalogo);
        scrollCat.setBorder(BorderFactory.createTitledBorder("Productos Disponibles con Existencia Vendible Vigente"));
        pTop.add(scrollCat, BorderLayout.CENTER);

        JPanel pAgregar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        pAgregar.add(new JLabel("Cantidad Solicitada:"));
        spnCantidadPedido = UiUtils.crearSpinnerEntero(1, 1, 10000, 1);
        spnCantidadPedido.setPreferredSize(new Dimension(80, 28));
        pAgregar.add(spnCantidadPedido);

        btnAgregarAlPedido = new JButton("Agregar al Carrito de Pedido");
        btnAgregarAlPedido.setFont(UiUtils.FONT_BOLD);
        btnAgregarAlPedido.setBackground(UiUtils.COLOR_PRIMARIO);
        btnAgregarAlPedido.setForeground(Color.WHITE);
        btnAgregarAlPedido.addActionListener(e -> agregarProductoSeleccionado());
        pAgregar.add(btnAgregarAlPedido);
        pTop.add(pAgregar, BorderLayout.SOUTH);

        p.add(pTop);

        // Sub-panel inferior: Carrito del cliente con desglose FEFO
        JPanel pBottom = new JPanel(new BorderLayout(5, 5));
        String[] cols = {"SKU", "Producto", "Cantidad", "Precio U.", "Subtotal", "Lotes Asignados FEFO (Caducidades)"};
        modeloCarritoCliente = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaCarritoCliente = new JTable(modeloCarritoCliente);
        UiUtils.darFormatoTabla(tablaCarritoCliente);
        tablaCarritoCliente.getColumnModel().getColumn(5).setPreferredWidth(320);

        JScrollPane scrollCar = new JScrollPane(tablaCarritoCliente);
        scrollCar.setBorder(BorderFactory.createTitledBorder("Mi Carrito de Autopedido"));
        pBottom.add(scrollCar, BorderLayout.CENTER);

        JPanel pAccionesCarrito = new JPanel(new BorderLayout(10, 5));
        lblTotalPedido = new JLabel("TOTAL PEDIDO: $0.00");
        lblTotalPedido.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        lblTotalPedido.setForeground(UiUtils.COLOR_PRIMARIO);

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnQuitar = new JButton("Quitar Producto");
        btnQuitar.addActionListener(e -> quitarItemCarrito());

        btnSimularFefo = new JButton("Simular Asignación de Lotes");
        btnSimularFefo.setFont(UiUtils.FONT_BOLD);
        btnSimularFefo.addActionListener(e -> ejecutarSimulacion(true));

        btnConfirmarPedido = new JButton("Confirmar Compra B2B");
        btnConfirmarPedido.setFont(UiUtils.FONT_BOLD);
        btnConfirmarPedido.setBackground(UiUtils.COLOR_VERDE_OK);
        btnConfirmarPedido.setForeground(Color.WHITE);
        btnConfirmarPedido.addActionListener(e -> confirmarCompraB2B());

        pBtns.add(btnQuitar);
        pBtns.add(btnSimularFefo);
        pBtns.add(btnConfirmarPedido);

        pAccionesCarrito.add(lblTotalPedido, BorderLayout.WEST);
        pAccionesCarrito.add(pBtns, BorderLayout.EAST);
        pBottom.add(pAccionesCarrito, BorderLayout.SOUTH);

        p.add(pBottom);
        return p;
    }

    private JPanel crearTabMisCompras() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        tablaMisCompras = new JTable();
        tablaMisCompras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaMisCompras);
        tablaMisCompras.getSelectionModel().addListSelectionListener(e -> {
            btnDescargarTicketCertificado.setEnabled(tablaMisCompras.getSelectedRow() >= 0);
        });

        JScrollPane scroll = new JScrollPane(tablaMisCompras);
        scroll.setBorder(BorderFactory.createTitledBorder("Historial de Compras y Trazabilidad de Lotes Recibidos"));
        p.add(scroll, BorderLayout.CENTER);

        JPanel pBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarMisCompras());

        btnDescargarTicketCertificado = new JButton("Descargar Nota de Venta y Certificado FEFO (PDF)");
        btnDescargarTicketCertificado.setFont(UiUtils.FONT_BOLD);
        btnDescargarTicketCertificado.setBackground(UiUtils.COLOR_PRIMARIO);
        btnDescargarTicketCertificado.setForeground(Color.WHITE);
        btnDescargarTicketCertificado.setEnabled(false);
        btnDescargarTicketCertificado.addActionListener(e -> descargarCertificadoVenta());

        pBotones.add(btnRefrescar);
        pBotones.add(btnDescargarTicketCertificado);
        p.add(pBotones, BorderLayout.SOUTH);

        return p;
    }

    private void cargarCatalogoProductos() {
        new SwingWorker<List<Producto>, Void>() {
            @Override
            protected List<Producto> doInBackground() throws Exception {
                return productoDAO.listarActivos();
            }

            @Override
            protected void done() {
                try {
                    List<Producto> lista = get();
                    String[] cols = {"ID", "SKU", "Producto", "Unidad", "Precio Unitario", "Existencia Vendible Vigente"};
                    DefaultTableModel m = new DefaultTableModel(cols, 0) {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    if (lista != null) {
                        for (Producto p : lista) {
                            m.addRow(new Object[]{
                                    p.getIdProducto(),
                                    p.getSku(),
                                    p.getNombre(),
                                    p.getUnidad(),
                                    UiUtils.formatoMoneda(p.getPrecioVenta()),
                                    p.getStockVigente() + " " + p.getUnidad()
                            });
                        }
                    }
                    tablaProductosCatalogo.setModel(m);
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void agregarProductoSeleccionado() {
        int fila = tablaProductosCatalogo.getSelectedRow();
        if (fila < 0) {
            UiUtils.mostrarMensajeAdvertencia(this, "Seleccione un producto de la tabla.");
            return;
        }

        int idProd = (int) tablaProductosCatalogo.getValueAt(fila, 0);
        int cant = (int) spnCantidadPedido.getValue();

        new SwingWorker<Producto, Void>() {
            @Override
            protected Producto doInBackground() throws Exception {
                return productoDAO.findById(idProd);
            }

            @Override
            protected void done() {
                try {
                    Producto p = get();
                    if (p != null) {
                        for (LineaVenta lv : carritoCliente) {
                            if (lv.getProducto().getIdProducto() == p.getIdProducto()) {
                                lv.setCantidad(lv.getCantidad() + cant);
                                refrescarCarritoVista();
                                ejecutarSimulacion(false);
                                return;
                            }
                        }
                        carritoCliente.add(new LineaVenta(p, cant, p.getPrecioVenta()));
                        refrescarCarritoVista();
                        ejecutarSimulacion(false);
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void refrescarCarritoVista() {
        modeloCarritoCliente.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;

        for (LineaVenta lv : carritoCliente) {
            total = total.add(lv.getSubtotal());
            modeloCarritoCliente.addRow(new Object[]{
                    lv.getProducto().getSku(),
                    lv.getProducto().getNombre(),
                    lv.getCantidad(),
                    UiUtils.formatoMoneda(lv.getPrecioUnitario()),
                    UiUtils.formatoMoneda(lv.getSubtotal()),
                    lv.getResumenLotes()
            });
        }
        lblTotalPedido.setText("TOTAL PEDIDO: " + UiUtils.formatoMoneda(total));
    }

    private void ejecutarSimulacion(boolean mostrarExito) {
        if (carritoCliente.isEmpty()) return;

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    ventaService.simularFefo(carritoCliente, LocalDate.now());
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                refrescarCarritoVista();
                if (error != null) {
                    UiUtils.mostrarMensajeError(PortalClientePanel.this, "Simulación FEFO:\n" + error.getMessage());
                } else if (mostrarExito) {
                    UiUtils.mostrarMensajeExito(PortalClientePanel.this, "Asignación FEFO calculada exitosamente.");
                }
            }
        }.execute();
    }

    private void quitarItemCarrito() {
        int f = tablaCarritoCliente.getSelectedRow();
        if (f >= 0 && f < carritoCliente.size()) {
            carritoCliente.remove(f);
            refrescarCarritoVista();
        }
    }

    private void confirmarCompraB2B() {
        if (carritoCliente.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "El carrito de pedido está vacío.");
            return;
        }

        Cliente cli = SesionActual.getInstance().getCliente();
        if (cli == null) {
            UiUtils.mostrarMensajeError(this, "No se encontró el registro de cliente asociado a esta sesión.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (LineaVenta lv : carritoCliente) total = total.add(lv.getSubtotal());

        if (!UiUtils.confirmar(this, "¿Confirmar autopedido por " + UiUtils.formatoMoneda(total) + "?", "Confirmar Autopedido")) {
            return;
        }

        btnConfirmarPedido.setEnabled(false);
        Integer idUsuario = SesionActual.getInstance().getUsuario() != null ? SesionActual.getInstance().getUsuario().getIdUsuario() : null;

        new SwingWorker<Integer, Void>() {
            private Exception error;

            @Override
            protected Integer doInBackground() {
                try {
                    return ventaService.registrarVenta(cli.getIdCliente(), cli.getNombre(), idUsuario, carritoCliente);
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                btnConfirmarPedido.setEnabled(true);
                if (error != null) {
                    UiUtils.mostrarMensajeError(PortalClientePanel.this, "No se pudo procesar el pedido:\n" + error.getMessage());
                    return;
                }
                try {
                    int idVenta = get();
                    String folio = String.format("V-%06d", idVenta);
                    carritoCliente.clear();
                    refrescarCarritoVista();
                    cargarCatalogoProductos();
                    cargarMisCompras();

                    int res = JOptionPane.showConfirmDialog(
                            PortalClientePanel.this,
                            "¡Autopedido registrado con éxito! Folio: " + folio + "\n\n¿Desea descargar ahora su Certificado de Lotes y Nota de Venta en PDF?",
                            "Pedido Confirmado",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    if (res == JOptionPane.YES_OPTION) {
                        descargarPdf(idVenta, folio);
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    public void cargarMisCompras() {
        Cliente cli = SesionActual.getInstance().getCliente();
        if (cli == null) return;

        new SwingWorker<ResultadoTabla, Void>() {
            @Override
            protected ResultadoTabla doInBackground() throws Exception {
                return consultaDAO.obtenerComprasCliente(cli.getIdCliente());
            }

            @Override
            protected void done() {
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaMisCompras.setModel(res.aModeloTablaPoblado());
                    }
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void descargarCertificadoVenta() {
        int fila = tablaMisCompras.getSelectedRow();
        if (fila < 0) return;

        int idVenta = (int) tablaMisCompras.getValueAt(fila, 0);
        String folio = String.valueOf(tablaMisCompras.getValueAt(fila, 1));
        descargarPdf(idVenta, folio);
    }

    private void descargarPdf(int idVenta, String folio) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Certificado_Lotes_" + folio + ".pdf"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                reportePdfService.generarTicketVentaPdf(idVenta, fc.getSelectedFile());
                UiUtils.mostrarMensajeExito(this, "Certificado PDF guardado correctamente.");
            } catch (Exception ex) {
                UiUtils.mostrarMensajeError(this, ex.getMessage());
            }
        }
    }
}
