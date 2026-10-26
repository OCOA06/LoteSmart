package com.lotesmart.vista;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateVetoPolicy;
import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.datos.ErroresBD;
import com.lotesmart.datos.LoteDAO;
import com.lotesmart.datos.ProductoDAO;
import com.lotesmart.datos.ProveedorDAO;
import com.lotesmart.datos.ResultadoTabla;
import com.lotesmart.modelo.EstadoLote;
import com.lotesmart.modelo.FiltroReporte;
import com.lotesmart.modelo.Lote;
import com.lotesmart.modelo.Producto;
import com.lotesmart.modelo.Proveedor;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.TipoMovimiento;
import com.lotesmart.negocio.AlmacenService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GestionLotesPanel extends JPanel {

    private final AlmacenService almacenService;
    private final LoteDAO loteDAO;
    private final ProductoDAO productoDAO;
    private final ProveedorDAO proveedorDAO;
    private final ConsultaDAO consultaDAO;

    // Componentes de Alta de Lote (Tab 1)
    private JComboBox<Producto> cmbProductosAlta;
    private JComboBox<Proveedor> cmbProveedoresAlta;
    private JTextField txtCodigoLote;
    private DatePicker dpFechaCaducidad;
    private JSpinner spnCantidadInicial;
    private JSpinner spnCostoUnitario;
    private JButton btnRegistrarLote;

    // Componentes de Gestión y Ajustes (Tab 2)
    private JTable tablaLotes;
    private JButton btnBloquear;
    private JButton btnDesbloquear;
    private JButton btnMerma;
    private JButton btnAjuste;
    private JButton btnEditarSinMov;
    private JButton btnEliminarSinMov;

    // Componentes de Kardex (Tab 3)
    private JTable tablaMovimientos;
    private JComboBox<String> cmbFiltroTipoMov;
    private JTextField txtBuscarLoteMov;

    public GestionLotesPanel() {
        this.almacenService = new AlmacenService();
        this.loteDAO = new LoteDAO();
        this.productoDAO = new ProductoDAO();
        this.proveedorDAO = new ProveedorDAO();
        this.consultaDAO = new ConsultaDAO();

        initComponents();
        cargarCatalogosAlta();
        cargarTablaLotes();
        cargarTablaMovimientos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();

        // TAB 1: RECEPCIÓN DE NUEVOS LOTES
        tabs.addTab("Recepción de Lotes Nuevos", crearPanelAltaLote());

        // TAB 2: INVENTARIO, BLOQUEO Y AJUSTES
        tabs.addTab("Control de Lotes y Ajustes", crearPanelGestionLotes());

        // TAB 3: KARDEX DE MOVIMIENTOS
        tabs.addTab("Kardex de Movimientos por Lote", crearPanelKardex());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel crearPanelAltaLote() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel formCard = new JPanel(new GridLayout(6, 2, 15, 15));
        UiUtils.aplicarEstiloCard(formCard);

        formCard.add(new JLabel("Producto a Recibir:"));
        cmbProductosAlta = new JComboBox<>();
        formCard.add(cmbProductosAlta);

        formCard.add(new JLabel("Proveedor de Origen:"));
        cmbProveedoresAlta = new JComboBox<>();
        formCard.add(cmbProveedoresAlta);

        formCard.add(new JLabel("Código de Lote (SKU/Lote):"));
        txtCodigoLote = new JTextField();
        UiUtils.aplicarFiltroMayusculas(txtCodigoLote, 30);
        formCard.add(txtCodigoLote);

        formCard.add(new JLabel("Fecha de Caducidad (Poka-Yoke > Hoy):"));
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        dateSettings.setAllowKeyboardEditing(false); // Cero captura libre de fechas
        dpFechaCaducidad = new DatePicker(dateSettings);
        dateSettings.setVetoPolicy(new DateVetoPolicy() {
            @Override
            public boolean isDateAllowed(LocalDate date) {
                // Veto de fechas pasadas o iguales a hoy
                return date != null && date.isAfter(LocalDate.now());
            }
        });
        dpFechaCaducidad.setDate(LocalDate.now().plusMonths(6));
        formCard.add(dpFechaCaducidad);

        formCard.add(new JLabel("Cantidad Inicial Recibida:"));
        spnCantidadInicial = UiUtils.crearSpinnerEntero(50, 1, 100000, 1);
        formCard.add(spnCantidadInicial);

        formCard.add(new JLabel("Costo Unitario de Compra ($):"));
        spnCostoUnitario = UiUtils.crearSpinnerDecimal(20.00, 0.01, 100000.00, 1.0);
        formCard.add(spnCostoUnitario);

        panel.add(formCard, BorderLayout.NORTH);

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRegistrarLote = new JButton("Registrar Entrada en Almacén");
        btnRegistrarLote.setFont(UiUtils.FONT_BOLD);
        btnRegistrarLote.setBackground(UiUtils.COLOR_PRIMARIO);
        btnRegistrarLote.setForeground(Color.WHITE);
        btnRegistrarLote.setPreferredSize(new Dimension(240, 36));
        btnRegistrarLote.addActionListener(e -> registrarNuevoLote());
        panelBoton.add(btnRegistrarLote);

        panel.add(panelBoton, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelGestionLotes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        tablaLotes = new JTable();
        tablaLotes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaLotes);
        tablaLotes.getSelectionModel().addListSelectionListener(e -> actualizarBotonesContextuales());

        JScrollPane scroll = new JScrollPane(tablaLotes);
        scroll.setBorder(BorderFactory.createTitledBorder("Catálogo de Lotes Activos y en Almacén"));
        panel.add(scroll, BorderLayout.CENTER);

        // Barra inferior de acciones contextuales Poka-Yoke
        JPanel barraAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarTablaLotes());
        barraAcciones.add(btnRefrescar);

        btnBloquear = new JButton("Bloqueo Sanitario");
        btnBloquear.setBackground(new Color(123, 31, 162));
        btnBloquear.setForeground(Color.WHITE);
        btnBloquear.addActionListener(e -> ejecutarBloqueoSanitario());
        barraAcciones.add(btnBloquear);

        btnDesbloquear = new JButton("Desbloquear Lote");
        btnDesbloquear.addActionListener(e -> ejecutarDesbloqueo());
        barraAcciones.add(btnDesbloquear);

        btnMerma = new JButton("Registrar Merma");
        btnMerma.addActionListener(e -> ejecutarRegistroMerma());
        barraAcciones.add(btnMerma);

        btnAjuste = new JButton("Ajuste Inventario (+ / -)");
        btnAjuste.addActionListener(e -> ejecutarAjusteInventario());
        barraAcciones.add(btnAjuste);

        btnEditarSinMov = new JButton("Corregir Captura");
        btnEditarSinMov.addActionListener(e -> ejecutarEdicionSinMovimientos());
        barraAcciones.add(btnEditarSinMov);

        btnEliminarSinMov = new JButton("Eliminar Lote");
        btnEliminarSinMov.setBackground(new Color(198, 40, 40));
        btnEliminarSinMov.setForeground(Color.WHITE);
        btnEliminarSinMov.addActionListener(e -> ejecutarEliminacionLote());
        barraAcciones.add(btnEliminarSinMov);

        // Control de permisos por rol: Operador no puede ajustar ni eliminar
        boolean esAdmin = SesionActual.getInstance().esAdmin();
        btnAjuste.setVisible(esAdmin);
        btnEditarSinMov.setVisible(esAdmin);
        btnEliminarSinMov.setVisible(esAdmin);

        panel.add(barraAcciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelKardex() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topKardex = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topKardex.add(new JLabel("Tipo de Movimiento:"));
        cmbFiltroTipoMov = new JComboBox<>(new String[]{"TODOS", "VENTA", "MERMA", "AJUSTE_NEGATIVO", "AJUSTE_POSITIVO", "DEVOLUCION"});
        cmbFiltroTipoMov.addActionListener(e -> cargarTablaMovimientos());
        topKardex.add(cmbFiltroTipoMov);

        topKardex.add(new JLabel("Buscar Lote:"));
        txtBuscarLoteMov = new JTextField(12);
        UiUtils.aplicarFiltroMayusculas(txtBuscarLoteMov, 20);
        topKardex.add(txtBuscarLoteMov);

        JButton btnBuscar = new JButton("Filtrar");
        btnBuscar.addActionListener(e -> cargarTablaMovimientos());
        topKardex.add(btnBuscar);

        panel.add(topKardex, BorderLayout.NORTH);

        tablaMovimientos = new JTable();
        UiUtils.darFormatoTabla(tablaMovimientos);
        panel.add(new JScrollPane(tablaMovimientos), BorderLayout.CENTER);

        return panel;
    }

    private void cargarCatalogosAlta() {
        new SwingWorker<Void, Void>() {
            private List<Producto> productos;
            private List<Proveedor> proveedores;

            @Override
            protected Void doInBackground() throws Exception {
                productos = productoDAO.listarActivos();
                proveedores = proveedorDAO.listarActivos();
                return null;
            }

            @Override
            protected void done() {
                cmbProductosAlta.removeAllItems();
                if (productos != null) {
                    for (Producto p : productos) cmbProductosAlta.addItem(p);
                }

                cmbProveedoresAlta.removeAllItems();
                if (proveedores != null) {
                    for (Proveedor pr : proveedores) cmbProveedoresAlta.addItem(pr);
                }
            }
        }.execute();
    }

    private void registrarNuevoLote() {
        Producto prod = (Producto) cmbProductosAlta.getSelectedItem();
        Proveedor prov = (Proveedor) cmbProveedoresAlta.getSelectedItem();

        if (prod == null || prov == null) {
            UiUtils.mostrarMensajeAdvertencia(this, "Debe seleccionar un producto y un proveedor válidos.");
            return;
        }

        String codigo = txtCodigoLote.getText().trim();
        if (codigo.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "Debe capturar el código de lote.");
            return;
        }

        LocalDate caducidad = dpFechaCaducidad.getDate();
        if (caducidad == null || !caducidad.isAfter(LocalDate.now())) {
            UiUtils.mostrarMensajeAdvertencia(this, "La fecha de caducidad debe ser posterior al día de hoy.");
            return;
        }

        int cantidad = (int) spnCantidadInicial.getValue();
        double costoDbl = ((Number) spnCostoUnitario.getValue()).doubleValue();
        BigDecimal costo = BigDecimal.valueOf(costoDbl);

        Lote lote = new Lote();
        lote.setIdProducto(prod.getIdProducto());
        lote.setIdProveedor(prov.getIdProveedor());
        lote.setCodigoLote(codigo);
        lote.setFechaCaducidad(caducidad);
        lote.setCantidadInicial(cantidad);
        lote.setCantidadDisponible(cantidad);
        lote.setCostoUnitario(costo);
        lote.setEstado(EstadoLote.VIGENTE);

        btnRegistrarLote.setEnabled(false);
        new SwingWorker<Integer, Void>() {
            private Exception error;

            @Override
            protected Integer doInBackground() {
                try {
                    return almacenService.registrarLote(lote);
                } catch (Exception e) {
                    this.error = e;
                    return 0;
                }
            }

            @Override
            protected void done() {
                btnRegistrarLote.setEnabled(true);
                if (error != null) {
                    UiUtils.mostrarMensajeError(GestionLotesPanel.this, "Error al dar de alta el lote:\n" + error.getMessage());
                } else {
                    UiUtils.mostrarMensajeExito(GestionLotesPanel.this, "¡Lote " + codigo + " registrado exitosamente en el sistema!");
                    txtCodigoLote.setText("");
                    cargarTablaLotes();
                }
            }
        }.execute();
    }

    public void cargarTablaLotes() {
        new SwingWorker<ResultadoTabla, Void>() {
            @Override
            protected ResultadoTabla doInBackground() throws Exception {
                return consultaDAO.obtenerLotesDetalle(null);
            }

            @Override
            protected void done() {
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaLotes.setModel(res.aModeloTablaPoblado());
                        // Render de semáforo
                        int colSem = -1;
                        for (int i = 0; i < tablaLotes.getColumnCount(); i++) {
                            if (tablaLotes.getColumnName(i).equalsIgnoreCase("Semáforo")) {
                                colSem = i;
                                break;
                            }
                        }
                        if (colSem >= 0) {
                            tablaLotes.getColumnModel().getColumn(colSem).setCellRenderer(new SemaforoCellRenderer());
                        }
                        actualizarBotonesContextuales();
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }

    private void actualizarBotonesContextuales() {
        int fila = tablaLotes.getSelectedRow();
        boolean seleccionada = fila >= 0;

        btnBloquear.setEnabled(seleccionada);
        btnDesbloquear.setEnabled(seleccionada);
        btnMerma.setEnabled(seleccionada);
        btnAjuste.setEnabled(seleccionada);
        btnEditarSinMov.setEnabled(seleccionada);
        btnEliminarSinMov.setEnabled(seleccionada);

        if (!seleccionada) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String estado = String.valueOf(tablaLotes.getValueAt(fila, 12));

        if ("BLOQUEADO".equalsIgnoreCase(estado)) {
            btnBloquear.setEnabled(false);
            btnDesbloquear.setEnabled(true);
        } else {
            btnBloquear.setEnabled(true);
            btnDesbloquear.setEnabled(false);
        }

        // Poka-Yoke en segundo plano: verificar si tiene movimientos para habilitar edición o borrado
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return loteDAO.tieneMovimientos(idLote);
            }

            @Override
            protected void done() {
                try {
                    boolean tieneMovs = get();
                    boolean esAdmin = SesionActual.getInstance().esAdmin();
                    btnEditarSinMov.setEnabled(!tieneMovs && esAdmin);
                    btnEliminarSinMov.setEnabled(!tieneMovs && esAdmin);
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }

    private void ejecutarBloqueoSanitario() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String codigo = String.valueOf(tablaLotes.getValueAt(fila, 1));

        String[] motivosPredefinidos = {
                "Sospecha de alteración o contaminación",
                "Alerta de retiro sanitario del fabricante",
                "Defecto en empaque primario o sello",
                "Falla en cadena de frío",
                "Inspección de calidad interna pendiente"
        };
        JComboBox<String> cmbMotivos = new JComboBox<>(motivosPredefinidos);
        JTextField txtNota = new JTextField();

        JPanel pForm = new JPanel(new GridLayout(4, 1, 5, 5));
        pForm.add(new JLabel("Seleccione motivo del bloqueo sanitario para lote " + codigo + ":"));
        pForm.add(cmbMotivos);
        pForm.add(new JLabel("Nota complementaria opcional:"));
        pForm.add(txtNota);

        int res = JOptionPane.showConfirmDialog(this, pForm, "Bloqueo Sanitario Preventivo", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String motivo = cmbMotivos.getSelectedItem() + (txtNota.getText().trim().isEmpty() ? "" : " - " + txtNota.getText().trim());
            try {
                almacenService.bloquearLote(idLote, motivo);
                UiUtils.mostrarMensajeExito(this, "El lote " + codigo + " ha sido puesto en BLOQUEO SANITARIO.");
                cargarTablaLotes();
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, e.getMessage());
            }
        }
    }

    private void ejecutarDesbloqueo() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String codigo = String.valueOf(tablaLotes.getValueAt(fila, 1));

        if (UiUtils.confirmar(this, "¿Levantar el bloqueo sanitario del lote " + codigo + " para reanudar su despacho FEFO?", "Desbloquear Lote")) {
            try {
                almacenService.desbloquearLote(idLote);
                UiUtils.mostrarMensajeExito(this, "El lote " + codigo + " ha sido desbloqueado.");
                cargarTablaLotes();
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, e.getMessage());
            }
        }
    }

    private void ejecutarRegistroMerma() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String codigo = String.valueOf(tablaLotes.getValueAt(fila, 1));
        int disp = (int) tablaLotes.getValueAt(fila, 7);

        if (disp <= 0) {
            UiUtils.mostrarMensajeAdvertencia(this, "Este lote no cuenta con existencia disponible para registrar mermas.");
            return;
        }

        JSpinner spnCantMerma = UiUtils.crearSpinnerEntero(1, 1, disp, 1);
        String[] motivosMerma = {"Producto roto por caída", "Empaque perforado/abierto", "Caducado en anaquel", "Humedad/Deterioro", "Muestra de laboratorio consumida"};
        JComboBox<String> cmbMotivo = new JComboBox<>(motivosMerma);

        JPanel pForm = new JPanel(new GridLayout(4, 1, 5, 5));
        pForm.add(new JLabel("Cantidad a merma (Máx. " + disp + "):"));
        pForm.add(spnCantMerma);
        pForm.add(new JLabel("Motivo de la merma:"));
        pForm.add(cmbMotivo);

        int res = JOptionPane.showConfirmDialog(this, pForm, "Registrar Merma en Lote " + codigo, JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            int cant = (int) spnCantMerma.getValue();
            String motivo = (String) cmbMotivo.getSelectedItem();
            try {
                almacenService.registrarMerma(idLote, cant, motivo);
                UiUtils.mostrarMensajeExito(this, "Merma de " + cant + " unidades registrada en lote " + codigo);
                cargarTablaLotes();
                cargarTablaMovimientos();
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, e.getMessage());
            }
        }
    }

    private void ejecutarAjusteInventario() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String codigo = String.valueOf(tablaLotes.getValueAt(fila, 1));
        int disp = (int) tablaLotes.getValueAt(fila, 7);

        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"AJUSTE_POSITIVO (+)", "AJUSTE_NEGATIVO (-)"});
        JSpinner spnCant = UiUtils.crearSpinnerEntero(1, 1, 10000, 1);
        String[] motivosAjuste = {"Conteo físico cíclico", "Corrección de sobrante de proveedor", "Faltante en auditoría física", "Aclaración administrativa"};
        JComboBox<String> cmbMotivo = new JComboBox<>(motivosAjuste);

        JPanel pForm = new JPanel(new GridLayout(6, 1, 5, 5));
        pForm.add(new JLabel("Dirección del Ajuste:"));
        pForm.add(cmbTipo);
        pForm.add(new JLabel("Cantidad a ajustar:"));
        pForm.add(spnCant);
        pForm.add(new JLabel("Motivo:"));
        pForm.add(cmbMotivo);

        int res = JOptionPane.showConfirmDialog(this, pForm, "Ajuste de Inventario en Lote " + codigo, JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            boolean esPositivo = cmbTipo.getSelectedIndex() == 0;
            int cant = (int) spnCant.getValue();
            String motivo = (String) cmbMotivo.getSelectedItem();
            try {
                almacenService.registrarAjuste(idLote, cant, esPositivo, motivo);
                UiUtils.mostrarMensajeExito(this, "Ajuste de inventario aplicado correctamente.");
                cargarTablaLotes();
                cargarTablaMovimientos();
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, e.getMessage());
            }
        }
    }

    private void ejecutarEdicionSinMovimientos() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String codigo = String.valueOf(tablaLotes.getValueAt(fila, 1));
        int cantActual = (int) tablaLotes.getValueAt(fila, 8);

        DatePickerSettings ds = new DatePickerSettings();
        DatePicker dp = new DatePicker(ds);
        ds.setVetoPolicy(date -> date != null && date.isAfter(LocalDate.now()));
        dp.setDate(LocalDate.now().plusMonths(3));

        JSpinner spnNuevaCant = UiUtils.crearSpinnerEntero(cantActual, 1, 100000, 1);
        JSpinner spnNuevoCosto = UiUtils.crearSpinnerDecimal(25.00, 0.01, 100000.0, 1.0);

        JPanel pForm = new JPanel(new GridLayout(6, 1, 5, 5));
        pForm.add(new JLabel("Nueva Fecha de Caducidad:"));
        pForm.add(dp);
        pForm.add(new JLabel("Nueva Cantidad Inicial:"));
        pForm.add(spnNuevaCant);
        pForm.add(new JLabel("Nuevo Costo Unitario:"));
        pForm.add(spnNuevoCosto);

        int res = JOptionPane.showConfirmDialog(this, pForm, "Corregir Captura Inicial - Lote " + codigo, JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                BigDecimal nuevoCosto = BigDecimal.valueOf(((Number) spnNuevoCosto.getValue()).doubleValue());
                almacenService.actualizarLoteSinMovimientos(idLote, dp.getDate(), nuevoCosto, (int) spnNuevaCant.getValue());
                UiUtils.mostrarMensajeExito(this, "Datos iniciales del lote actualizados correctamente.");
                cargarTablaLotes();
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, e.getMessage());
            }
        }
    }

    private void ejecutarEliminacionLote() {
        int fila = tablaLotes.getSelectedRow();
        if (fila < 0) return;

        int idLote = (int) tablaLotes.getValueAt(fila, 0);
        String codigo = String.valueOf(tablaLotes.getValueAt(fila, 1));

        if (UiUtils.confirmar(this, "¿Está seguro de eliminar permanentemente el lote " + codigo + "?\nEsta acción solo es permitida si no tiene movimientos.", "Eliminar Lote")) {
            try {
                almacenService.eliminarLote(idLote);
                UiUtils.mostrarMensajeExito(this, "Lote eliminado correctamente.");
                cargarTablaLotes();
            } catch (Exception e) {
                UiUtils.mostrarMensajeError(this, e.getMessage());
            }
        }
    }

    public void cargarTablaMovimientos() {
        String tipo = (String) cmbFiltroTipoMov.getSelectedItem();
        String busqueda = txtBuscarLoteMov.getText().trim();

        new SwingWorker<ResultadoTabla, Void>() {
            @Override
            protected ResultadoTabla doInBackground() throws Exception {
                FiltroReporte f = new FiltroReporte();
                if (tipo != null && !tipo.equalsIgnoreCase("TODOS")) {
                    f.setTipoMovimiento(TipoMovimiento.fromString(tipo));
                }
                if (!busqueda.isEmpty()) {
                    f.setCodigoLote(busqueda);
                }
                return consultaDAO.obtenerMovimientos(f);
            }

            @Override
            protected void done() {
                try {
                    ResultadoTabla res = get();
                    if (res != null) {
                        tablaMovimientos.setModel(res.aModeloTablaPoblado());
                    }
                } catch (Exception e) {
                    // ignorar
                }
            }
        }.execute();
    }
}
