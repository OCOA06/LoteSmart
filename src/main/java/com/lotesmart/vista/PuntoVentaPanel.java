package com.lotesmart.vista;

import com.lotesmart.datos.ClienteDAO;
import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.datos.ErroresBD;
import com.lotesmart.datos.ProductoDAO;
import com.lotesmart.datos.ResultadoTabla;
import com.lotesmart.modelo.Asignacion;
import com.lotesmart.modelo.Cliente;
import com.lotesmart.modelo.LineaVenta;
import com.lotesmart.modelo.Producto;
import com.lotesmart.modelo.RolUsuario;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.Venta;
import com.lotesmart.negocio.ReglaNegocioException;
import com.lotesmart.negocio.VentaService;
import com.lotesmart.reportes.ReportePdfService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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

public class PuntoVentaPanel extends JPanel {

    private final VentaService ventaService;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final ConsultaDAO consultaDAO;
    private final ReportePdfService reportePdfService;

    // Componentes del Carrito
    private JComboBox<Cliente> cmbClientes;
    private JComboBox<Producto> cmbProductos;
    private JSpinner spnCantidad;
    private JButton btnAgregar;
    private JTable tablaCarrito;
    private DefaultTableModel modeloCarrito;
    private List<LineaVenta> carrito;

    private JLabel lblTotalVenta;
    private JButton btnSimularFefo;
    private JButton btnCobrar;
    private JButton btnEliminarItem;
    private JButton btnLimpiarCarrito;

    // Componentes del Historial de Ventas
    private JTable tablaHistorialVentas;
    private JButton btnVerDetalleVenta;
    private JButton btnDescargarTicketPdf;
    private JButton btnCancelarVenta;

    public PuntoVentaPanel() {
        this.ventaService = new VentaService();
        this.productoDAO = new ProductoDAO();
        this.clienteDAO = new ClienteDAO();
        this.consultaDAO = new ConsultaDAO();
        this.reportePdfService = new ReportePdfService();
        this.carrito = new ArrayList<>();

        initComponents();
        cargarCatalogos();
        cargarHistorialVentas();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTabbedPane subTabs = new JTabbedPane();

        // SUB-TAB 1: CAJA REGISTRADORA Y DESPACHO FEFO
        JPanel panelCaja = new JPanel(new BorderLayout(10, 10));

        // Encabezado de Cliente y Selección de Producto
        JPanel panelSuperior = new JPanel(new GridLayout(2, 1, 5, 5));
        UiUtils.aplicarEstiloCard(panelSuperior);

        // Fila 1: Cliente
        JPanel filaCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filaCliente.setOpaque(false);
        filaCliente.add(new JLabel("Cliente Destino:"));
        cmbClientes = new JComboBox<>();
        cmbClientes.setPreferredSize(new Dimension(380, 30));
        filaCliente.add(cmbClientes);
        panelSuperior.add(filaCliente);

        // Fila 2: Producto y Cantidad Poka-Yoke
        JPanel filaProducto = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filaProducto.setOpaque(false);
        filaProducto.add(new JLabel("Seleccionar Producto:"));
        cmbProductos = new JComboBox<>();
        cmbProductos.setPreferredSize(new Dimension(350, 30));
        filaProducto.add(cmbProductos);

        filaProducto.add(new JLabel("Cantidad:"));
        spnCantidad = UiUtils.crearSpinnerEntero(1, 1, 10000, 1);
        spnCantidad.setPreferredSize(new Dimension(80, 30));
        filaProducto.add(spnCantidad);

        btnAgregar = new JButton("Agregar al Carrito");
        btnAgregar.setFont(UiUtils.FONT_BOLD);
        btnAgregar.setBackground(UiUtils.COLOR_PRIMARIO);
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.addActionListener(e -> agregarProductoAlCarrito());
        filaProducto.add(btnAgregar);

        panelSuperior.add(filaProducto);
        panelCaja.add(panelSuperior, BorderLayout.NORTH);

        // Tabla del Carrito
        String[] columnasCarrito = {"SKU", "Producto", "Cant.", "Precio Unit.", "Subtotal", "Asignación FEFO (Lotes y Caducidades)"};
        modeloCarrito = new DefaultTableModel(columnasCarrito, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaCarrito = new JTable(modeloCarrito);
        UiUtils.darFormatoTabla(tablaCarrito);
        tablaCarrito.getColumnModel().getColumn(5).setPreferredWidth(320);

        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        scrollCarrito.setBorder(BorderFactory.createTitledBorder("Partidas de Venta y Desglose de Despacho FEFO"));
        panelCaja.add(scrollCarrito, BorderLayout.CENTER);

        // Barra inferior con Total y Acciones
        JPanel panelInferior = new JPanel(new BorderLayout(10, 10));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnSimularFefo = new JButton("Simular Asignación FEFO");
        btnSimularFefo.setFont(UiUtils.FONT_BOLD);
        btnSimularFefo.addActionListener(e -> ejecutarSimulacionFefo(true));

        btnEliminarItem = new JButton("Eliminar Seleccionado");
        btnEliminarItem.addActionListener(e -> eliminarItemCarrito());

        btnLimpiarCarrito = new JButton("Limpiar Carrito");
        btnLimpiarCarrito.addActionListener(e -> limpiarCarrito());

        panelBotonesIzquierda.add(btnSimularFefo);
        panelBotonesIzquierda.add(btnEliminarItem);
        panelBotonesIzquierda.add(btnLimpiarCarrito);

        JPanel panelCobroDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        lblTotalVenta = new JLabel("TOTAL: $0.00");
        lblTotalVenta.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        lblTotalVenta.setForeground(UiUtils.COLOR_PRIMARIO);

        btnCobrar = new JButton("Cobrar y Registrar Venta");
        btnCobrar.setFont(UiUtils.FONT_BOLD);
        btnCobrar.setBackground(UiUtils.COLOR_VERDE_OK);
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.setPreferredSize(new Dimension(210, 36));
        btnCobrar.addActionListener(e -> procesarCobroVenta());

        panelCobroDerecha.add(lblTotalVenta);
        panelCobroDerecha.add(btnCobrar);

        panelInferior.add(panelBotonesIzquierda, BorderLayout.WEST);
        panelInferior.add(panelCobroDerecha, BorderLayout.EAST);
        panelCaja.add(panelInferior, BorderLayout.SOUTH);

        subTabs.addTab("Caja y Venta FEFO", panelCaja);

        // SUB-TAB 2: HISTORIAL Y CANCELACIÓN DE VENTAS
        JPanel panelHistorial = new JPanel(new BorderLayout(10, 10));
        panelHistorial.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tablaHistorialVentas = new JTable();
        tablaHistorialVentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaHistorialVentas);
        tablaHistorialVentas.getSelectionModel().addListSelectionListener(e -> actualizarEstadoBotonesHistorial());

        JScrollPane scrollHistorial = new JScrollPane(tablaHistorialVentas);
        scrollHistorial.setBorder(BorderFactory.createTitledBorder("Registro de Ventas en la Base de Datos"));
        panelHistorial.add(scrollHistorial, BorderLayout.CENTER);

        JPanel barraAccionesHistorial = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnRefrescarHistorial = new JButton("Refrescar");
        btnRefrescarHistorial.addActionListener(e -> cargarHistorialVentas());

        btnVerDetalleVenta = new JButton("Ver Lotes Surtidos");
        btnVerDetalleVenta.addActionListener(e -> verLotesSurtidosVenta());

        btnDescargarTicketPdf = new JButton("Descargar Ticket PDF");
        btnDescargarTicketPdf.setFont(UiUtils.FONT_BOLD);
        btnDescargarTicketPdf.addActionListener(e -> descargarTicketVentaSeleccionada());

        btnCancelarVenta = new JButton("Cancelar Venta (Devolución a Lotes)");
        btnCancelarVenta.setBackground(new Color(220, 53, 69));
        btnCancelarVenta.setForeground(Color.WHITE);
        btnCancelarVenta.addActionListener(e -> ejecutarCancelacionVenta());

        // Principio de seguridad por rol: Operador no puede cancelar ventas
        boolean puedeCancelar = SesionActual.getInstance().esAdmin();
        btnCancelarVenta.setVisible(puedeCancelar);

        barraAccionesHistorial.add(btnRefrescarHistorial);
        barraAccionesHistorial.add(btnVerDetalleVenta);
        barraAccionesHistorial.add(btnDescargarTicketPdf);
        barraAccionesHistorial.add(btnCancelarVenta);
        panelHistorial.add(barraAccionesHistorial, BorderLayout.SOUTH);

        subTabs.addTab("Historial y Cancelaciones", panelHistorial);

        add(subTabs, BorderLayout.CENTER);
    }

    public void cargarCatalogos() {
        new SwingWorker<Void, Void>() {
            private List<Cliente> clientes;
            private List<Producto> productos;

            @Override
            protected Void doInBackground() throws Exception {
                clientes = clienteDAO.listarActivos();
                productos = productoDAO.listarActivos();
                return null;
            }

            @Override
            protected void done() {
                cmbClientes.removeAllItems();
                if (clientes != null) {
                    for (Cliente c : clientes) {
                        cmbClientes.addItem(c);
                    }
                }

                cmbProductos.removeAllItems();
                if (productos != null) {
                    for (Producto p : productos) {
                        cmbProductos.addItem(p);
                    }
                }
            }
        }.execute();
    }

    private void agregarProductoAlCarrito() {
        Producto p = (Producto) cmbProductos.getSelectedItem();
        if (p == null) {
            UiUtils.mostrarMensajeAdvertencia(this, "Debe seleccionar un producto.");
            return;
        }

        int cantidad = (int) spnCantidad.getValue();
        if (cantidad <= 0) {
            UiUtils.mostrarMensajeAdvertencia(this, "La cantidad debe ser mayor a cero.");
            return;
        }

        // Si ya está en el carrito, sumar cantidad
        for (LineaVenta lv : carrito) {
            if (lv.getProducto().getIdProducto() == p.getIdProducto()) {
                lv.setCantidad(lv.getCantidad() + cantidad);
                actualizarVistaCarrito();
                ejecutarSimulacionFefo(false);
                return;
            }
        }

        carrito.add(new LineaVenta(p, cantidad, p.getPrecioVenta()));
        actualizarVistaCarrito();
        ejecutarSimulacionFefo(false);
    }

    private void actualizarVistaCarrito() {
        modeloCarrito.setRowCount(0);
        BigDecimal granTotal = BigDecimal.ZERO;

        for (LineaVenta lv : carrito) {
            granTotal = granTotal.add(lv.getSubtotal());
            modeloCarrito.addRow(new Object[]{
                    lv.getProducto().getSku(),
                    lv.getProducto().getNombre(),
                    lv.getCantidad(),
                    UiUtils.formatoMoneda(lv.getPrecioUnitario()),
                    UiUtils.formatoMoneda(lv.getSubtotal()),
                    lv.getResumenLotes()
            });
        }
        lblTotalVenta.setText("TOTAL: " + UiUtils.formatoMoneda(granTotal));
    }

    private void ejecutarSimulacionFefo(boolean mostrarDialogo) {
        if (carrito.isEmpty()) {
            if (mostrarDialogo) UiUtils.mostrarMensajeAdvertencia(this, "El carrito está vacío.");
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    ventaService.simularFefo(carrito, LocalDate.now());
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                actualizarVistaCarrito();
                if (error != null) {
                    UiUtils.mostrarMensajeError(PuntoVentaPanel.this, "Simulación FEFO rechazada:\n" + error.getMessage());
                } else if (mostrarDialogo) {
                    UiUtils.mostrarMensajeExito(PuntoVentaPanel.this, "¡Simulación FEFO exitosa!\nLas partidas se han asignado a los lotes más próximos a caducar.");
                }
            }
        }.execute();
    }

    private void procesarCobroVenta() {
        if (carrito.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "El carrito no contiene productos.");
            return;
        }

        Cliente cliente = (Cliente) cmbClientes.getSelectedItem();
        if (cliente == null) {
            UiUtils.mostrarMensajeAdvertencia(this, "Debe seleccionar un cliente.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (LineaVenta lv : carrito) total = total.add(lv.getSubtotal());

        String msg = String.format("¿Confirmar cobro de venta por %s a favor de '%s'?", UiUtils.formatoMoneda(total), cliente.getNombre());
        if (!UiUtils.confirmar(this, msg, "Confirmar Venta")) {
            return;
        }

        btnCobrar.setEnabled(false);
        Integer idUsuario = SesionActual.getInstance().getUsuario() != null ? SesionActual.getInstance().getUsuario().getIdUsuario() : null;

        new SwingWorker<Integer, Void>() {
            private Exception error;

            @Override
            protected Integer doInBackground() {
                try {
                    return ventaService.registrarVenta(cliente.getIdCliente(), cliente.getNombre(), idUsuario, carrito);
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                btnCobrar.setEnabled(true);
                if (error != null) {
                    UiUtils.mostrarMensajeError(PuntoVentaPanel.this, "No se pudo registrar la venta:\n" + error.getMessage());
                    return;
                }
                try {
                    int idVentaGenerada = get();
                    String folio = String.format("V-%06d", idVentaGenerada);
                    limpiarCarrito();
                    cargarHistorialVentas();

                    int opcion = JOptionPane.showConfirmDialog(
                            PuntoVentaPanel.this,
                            "¡Venta registrada exitosamente con Folio " + folio + "!\n\n¿Desea generar y guardar el Ticket / Nota de Venta FEFO en PDF ahora?",
                            "Venta Completada",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    if (opcion == JOptionPane.YES_OPTION) {
                        descargarTicketPdf(idVentaGenerada, folio);
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }

    private void eliminarItemCarrito() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila >= 0 && fila < carrito.size()) {
            carrito.remove(fila);
            actualizarVistaCarrito();
        }
    }

    private void limpiarCarrito() {
        carrito.clear();
        actualizarVistaCarrito();
    }

    public void cargarHistorialVentas() {
        new SwingWorker<ResultadoTabla, Void>() {
            @Override
            protected ResultadoTabla doInBackground() throws Exception {
                return consultaDAO.obtenerVentas(null);
            }

            @Override
            protected void done() {
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaHistorialVentas.setModel(res.aModeloTablaPoblado());
                        actualizarEstadoBotonesHistorial();
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }

    private void actualizarEstadoBotonesHistorial() {
        int fila = tablaHistorialVentas.getSelectedRow();
        boolean seleccionada = fila >= 0;
        btnVerDetalleVenta.setEnabled(seleccionada);
        btnDescargarTicketPdf.setEnabled(seleccionada);

        if (!seleccionada) {
            btnCancelarVenta.setEnabled(false);
            return;
        }

        // Comprobar estado de la venta en la fila seleccionada
        int colEstado = -1;
        for (int i = 0; i < tablaHistorialVentas.getColumnCount(); i++) {
            if (tablaHistorialVentas.getColumnName(i).equalsIgnoreCase("Estado")) {
                colEstado = i;
                break;
            }
        }

        boolean esCancelada = false;
        if (colEstado >= 0) {
            Object val = tablaHistorialVentas.getValueAt(fila, colEstado);
            if (val != null && val.toString().equalsIgnoreCase("CANCELADA")) {
                esCancelada = true;
            }
        }

        // Poka-Yoke: Botón de cancelar deshabilitado si ya está cancelada o no es admin
        btnCancelarVenta.setEnabled(!esCancelada && SesionActual.getInstance().esAdmin());
    }

    private void verLotesSurtidosVenta() {
        int fila = tablaHistorialVentas.getSelectedRow();
        if (fila < 0) return;

        int idVenta = (int) tablaHistorialVentas.getValueAt(fila, 0);
        String folio = String.valueOf(tablaHistorialVentas.getValueAt(fila, 1));

        new SwingWorker<List<Asignacion>, Void>() {
            @Override
            protected List<Asignacion> doInBackground() throws Exception {
                return consultaDAO.obtenerMovimientosVenta(idVenta);
            }

            @Override
            protected void done() {
                try {
                    List<Asignacion> asignaciones = get();
                    if (asignaciones == null || asignaciones.isEmpty()) {
                        UiUtils.mostrarMensajeAdvertencia(PuntoVentaPanel.this, "No hay detalle de lotes surtidos.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("Lotes surtidos bajo regla FEFO para la venta ").append(folio).append(":\n\n");
                    for (Asignacion a : asignaciones) {
                        sb.append("• Lote: ").append(a.getCodigoLote())
                                .append(" | Caducidad: ").append(a.getFechaCaducidad())
                                .append(" | Cantidad: ").append(a.getCantidad()).append(" uds\n");
                    }
                    JOptionPane.showMessageDialog(PuntoVentaPanel.this, sb.toString(), "Trazabilidad de Venta " + folio, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    UiUtils.mostrarMensajeError(PuntoVentaPanel.this, "Error al consultar lotes: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void descargarTicketVentaSeleccionada() {
        int fila = tablaHistorialVentas.getSelectedRow();
        if (fila < 0) return;

        int idVenta = (int) tablaHistorialVentas.getValueAt(fila, 0);
        String folio = String.valueOf(tablaHistorialVentas.getValueAt(fila, 1));
        descargarTicketPdf(idVenta, folio);
    }

    private void descargarTicketPdf(int idVenta, String folio) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Ticket_" + folio + ".pdf"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File destino = fc.getSelectedFile();
            try {
                reportePdfService.generarTicketVentaPdf(idVenta, destino);
                UiUtils.mostrarMensajeExito(this, "Ticket PDF generado con éxito en:\n" + destino.getAbsolutePath());
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, "Error al generar ticket PDF: " + e.getMessage());
            }
        }
    }

    private void ejecutarCancelacionVenta() {
        int fila = tablaHistorialVentas.getSelectedRow();
        if (fila < 0) return;

        int idVenta = (int) tablaHistorialVentas.getValueAt(fila, 0);
        String folio = String.valueOf(tablaHistorialVentas.getValueAt(fila, 1));

        String motivo = JOptionPane.showInputDialog(
                this,
                "Ingrese el motivo de cancelación formal para " + folio + ":\n(Cada unidad surtida regresará a su lote original)",
                "Cancelar Venta",
                JOptionPane.WARNING_MESSAGE
        );

        if (motivo == null || motivo.trim().isEmpty()) {
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    ventaService.cancelarVenta(idVenta, motivo.trim());
                } catch (Exception e) {
                    this.error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    UiUtils.mostrarMensajeError(PuntoVentaPanel.this, "Error al cancelar la venta:\n" + error.getMessage());
                } else {
                    UiUtils.mostrarMensajeExito(PuntoVentaPanel.this, "La venta " + folio + " fue cancelada y sus lotes fueron repuestos.");
                    cargarHistorialVentas();
                }
            }
        }.execute();
    }
}
