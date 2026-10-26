package com.lotesmart.vista;

import com.lotesmart.datos.ClienteDAO;
import com.lotesmart.datos.ProductoDAO;
import com.lotesmart.datos.ProveedorDAO;
import com.lotesmart.datos.UsuarioDAO;
import com.lotesmart.modelo.Cliente;
import com.lotesmart.modelo.Producto;
import com.lotesmart.modelo.Proveedor;
import com.lotesmart.modelo.RolUsuario;
import com.lotesmart.modelo.Usuario;
import com.lotesmart.negocio.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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
import java.math.BigDecimal;
import java.util.List;

public class CatalogosPanel extends JPanel {

    private final ProductoDAO productoDAO;
    private final ProveedorDAO proveedorDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioDAO usuarioDAO;

    // Tablas
    private JTable tablaProductos;
    private JTable tablaProveedores;
    private JTable tablaClientes;
    private JTable tablaUsuarios;

    // Campos Producto
    private JTextField txtProdSku;
    private JTextField txtProdNombre;
    private JComboBox<String> cmbProdUnidad;
    private JSpinner spnProdPrecio;
    private JSpinner spnProdStockMin;
    private JSpinner spnProdDiasMin;
    private JCheckBox chkProdActivo;
    private int idProductoSeleccionado = 0;

    // Campos Proveedor
    private JTextField txtProvRfc;
    private JTextField txtProvNombre;
    private JTextField txtProvTel;
    private JCheckBox chkProvActivo;
    private int idProveedorSeleccionado = 0;

    // Campos Cliente
    private JTextField txtCliRfc;
    private JTextField txtCliNombre;
    private JTextField txtCliTel;
    private JTextField txtCliCorreo;
    private JComboBox<Usuario> cmbCliUsuario;
    private JCheckBox chkCliActivo;
    private int idClienteSeleccionado = 0;

    // Campos Usuario
    private JTextField txtUsrUsername;
    private JTextField txtUsrNombre;
    private JComboBox<RolUsuario> cmbUsrRol;
    private JPasswordField txtUsrPass;
    private JCheckBox chkUsrActivo;
    private int idUsuarioSeleccionado = 0;

    public CatalogosPanel() {
        this.productoDAO = new ProductoDAO();
        this.proveedorDAO = new ProveedorDAO();
        this.clienteDAO = new ClienteDAO();
        this.usuarioDAO = new UsuarioDAO();

        initComponents();
        cargarTodosLosCatalogos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Productos", crearPanelProductos());
        tabs.addTab("Proveedores", crearPanelProveedores());
        tabs.addTab("Clientes", crearPanelClientes());
        tabs.addTab("Usuarios del Sistema", crearPanelUsuarios());

        add(tabs, BorderLayout.CENTER);
    }

    // 1. PRODUCTOS
    private JPanel crearPanelProductos() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(4, 4, 8, 8));
        UiUtils.aplicarEstiloCard(form);

        form.add(new JLabel("SKU:"));
        txtProdSku = new JTextField();
        UiUtils.aplicarFiltroMayusculas(txtProdSku, 20);
        form.add(txtProdSku);

        form.add(new JLabel("Nombre:"));
        txtProdNombre = new JTextField();
        form.add(txtProdNombre);

        form.add(new JLabel("Unidad de Medida:"));
        cmbProdUnidad = new JComboBox<>(new String[]{"PIEZA", "CAJA", "LITRO", "KILO", "FRASCO", "PAQUETE", "LATA"});
        form.add(cmbProdUnidad);

        form.add(new JLabel("Precio Venta ($):"));
        spnProdPrecio = UiUtils.crearSpinnerDecimal(25.0, 0.01, 100000.0, 1.0);
        form.add(spnProdPrecio);

        form.add(new JLabel("Stock Mínimo:"));
        spnProdStockMin = UiUtils.crearSpinnerEntero(10, 0, 100000, 1);
        form.add(spnProdStockMin);

        form.add(new JLabel("Días Mínimos Venta:"));
        spnProdDiasMin = UiUtils.crearSpinnerEntero(7, 0, 3650, 1);
        form.add(spnProdDiasMin);

        form.add(new JLabel("Estado:"));
        chkProdActivo = new JCheckBox("Activo", true);
        form.add(chkProdActivo);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton btnNuevo = new JButton("Nuevo");
        btnNuevo.addActionListener(e -> limpiarFormProducto());
        JButton btnGuardar = new JButton("Guardar Producto");
        btnGuardar.setFont(UiUtils.FONT_BOLD);
        btnGuardar.setBackground(UiUtils.COLOR_PRIMARIO);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> guardarProducto());
        pBtn.add(btnNuevo);
        pBtn.add(btnGuardar);
        form.add(pBtn);

        p.add(form, BorderLayout.NORTH);

        tablaProductos = new JTable();
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaProductos);
        tablaProductos.getSelectionModel().addListSelectionListener(e -> seleccionarProductoTabla());
        p.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        return p;
    }

    // 2. PROVEEDORES
    private JPanel crearPanelProveedores() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        UiUtils.aplicarEstiloCard(form);

        form.add(new JLabel("RFC (12 o 13 caracteres):"));
        txtProvRfc = new JTextField();
        UiUtils.aplicarValidacionRfcVisual(txtProvRfc);
        form.add(txtProvRfc);

        form.add(new JLabel("Razón Social / Nombre:"));
        txtProvNombre = new JTextField();
        form.add(txtProvNombre);

        form.add(new JLabel("Teléfono:"));
        txtProvTel = new JTextField();
        form.add(txtProvTel);

        form.add(new JLabel("Estado:"));
        chkProvActivo = new JCheckBox("Activo", true);
        form.add(chkProvActivo);

        form.add(new JLabel(""));
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton btnNuevo = new JButton("Nuevo");
        btnNuevo.addActionListener(e -> limpiarFormProveedor());
        JButton btnGuardar = new JButton("Guardar Proveedor");
        btnGuardar.setFont(UiUtils.FONT_BOLD);
        btnGuardar.setBackground(UiUtils.COLOR_PRIMARIO);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> guardarProveedor());
        pBtn.add(btnNuevo);
        pBtn.add(btnGuardar);
        form.add(pBtn);

        p.add(form, BorderLayout.NORTH);

        tablaProveedores = new JTable();
        tablaProveedores.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaProveedores);
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> seleccionarProveedorTabla());
        p.add(new JScrollPane(tablaProveedores), BorderLayout.CENTER);

        return p;
    }

    // 3. CLIENTES
    private JPanel crearPanelClientes() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(4, 4, 8, 8));
        UiUtils.aplicarEstiloCard(form);

        form.add(new JLabel("RFC (12 o 13 caracteres):"));
        txtCliRfc = new JTextField();
        UiUtils.aplicarValidacionRfcVisual(txtCliRfc);
        form.add(txtCliRfc);

        form.add(new JLabel("Razón Social / Nombre:"));
        txtCliNombre = new JTextField();
        form.add(txtCliNombre);

        form.add(new JLabel("Teléfono:"));
        txtCliTel = new JTextField();
        form.add(txtCliTel);

        form.add(new JLabel("Correo Electrónico:"));
        txtCliCorreo = new JTextField();
        form.add(txtCliCorreo);

        form.add(new JLabel("Usuario Portal B2B:"));
        cmbCliUsuario = new JComboBox<>();
        form.add(cmbCliUsuario);

        form.add(new JLabel("Estado:"));
        chkCliActivo = new JCheckBox("Activo", true);
        form.add(chkCliActivo);

        form.add(new JLabel(""));
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton btnNuevo = new JButton("Nuevo");
        btnNuevo.addActionListener(e -> limpiarFormCliente());
        JButton btnGuardar = new JButton("Guardar Cliente");
        btnGuardar.setFont(UiUtils.FONT_BOLD);
        btnGuardar.setBackground(UiUtils.COLOR_PRIMARIO);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> guardarCliente());
        pBtn.add(btnNuevo);
        pBtn.add(btnGuardar);
        form.add(pBtn);

        p.add(form, BorderLayout.NORTH);

        tablaClientes = new JTable();
        tablaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaClientes);
        tablaClientes.getSelectionModel().addListSelectionListener(e -> seleccionarClienteTabla());
        p.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);

        return p;
    }

    // 4. USUARIOS
    private JPanel crearPanelUsuarios() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        UiUtils.aplicarEstiloCard(form);

        form.add(new JLabel("Nombre de Usuario:"));
        txtUsrUsername = new JTextField();
        UiUtils.aplicarFiltroMayusculas(txtUsrUsername, 30);
        form.add(txtUsrUsername);

        form.add(new JLabel("Nombre Completo:"));
        txtUsrNombre = new JTextField();
        form.add(txtUsrNombre);

        form.add(new JLabel("Rol de Acceso:"));
        cmbUsrRol = new JComboBox<>(RolUsuario.values());
        form.add(cmbUsrRol);

        form.add(new JLabel("Contraseña (dejar vacío si no cambia):"));
        txtUsrPass = new JPasswordField();
        form.add(txtUsrPass);

        form.add(new JLabel("Estado:"));
        chkUsrActivo = new JCheckBox("Activo", true);
        form.add(chkUsrActivo);

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton btnNuevo = new JButton("Nuevo");
        btnNuevo.addActionListener(e -> limpiarFormUsuario());
        JButton btnGuardar = new JButton("Guardar Usuario");
        btnGuardar.setFont(UiUtils.FONT_BOLD);
        btnGuardar.setBackground(UiUtils.COLOR_PRIMARIO);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> guardarUsuario());
        pBtn.add(btnNuevo);
        pBtn.add(btnGuardar);
        form.add(pBtn);

        p.add(form, BorderLayout.NORTH);

        tablaUsuarios = new JTable();
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiUtils.darFormatoTabla(tablaUsuarios);
        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> seleccionarUsuarioTabla());
        p.add(new JScrollPane(tablaUsuarios), BorderLayout.CENTER);

        return p;
    }

    public void cargarTodosLosCatalogos() {
        cargarProductos();
        cargarProveedores();
        cargarClientes();
        cargarUsuarios();
    }

    // PRODUCTOS LOGIC
    private void cargarProductos() {
        new SwingWorker<List<Producto>, Void>() {
            @Override
            protected List<Producto> doInBackground() throws Exception {
                return productoDAO.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    List<Producto> lista = get();
                    String[] cols = {"ID", "SKU", "Nombre", "Unidad", "Precio Venta", "Stock Mín.", "Días Mín.", "Activo"};
                    DefaultTableModel m = new DefaultTableModel(cols, 0) {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    if (lista != null) {
                        for (Producto pr : lista) {
                            m.addRow(new Object[]{pr.getIdProducto(), pr.getSku(), pr.getNombre(), pr.getUnidad(),
                                    UiUtils.formatoMoneda(pr.getPrecioVenta()), pr.getStockMinimo(), pr.getDiasMinimosVenta(),
                                    pr.isActivo() ? "SÍ" : "NO"});
                        }
                    }
                    tablaProductos.setModel(m);
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void guardarProducto() {
        String sku = txtProdSku.getText().trim();
        String nom = txtProdNombre.getText().trim();
        if (sku.isEmpty() || nom.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "SKU y Nombre son obligatorios.");
            return;
        }

        Producto p = new Producto();
        p.setIdProducto(idProductoSeleccionado);
        p.setSku(sku);
        p.setNombre(nom);
        p.setUnidad((String) cmbProdUnidad.getSelectedItem());
        p.setPrecioVenta(BigDecimal.valueOf(((Number) spnProdPrecio.getValue()).doubleValue()));
        p.setStockMinimo((int) spnProdStockMin.getValue());
        p.setDiasMinimosVenta((int) spnProdDiasMin.getValue());
        p.setActivo(chkProdActivo.isSelected());

        try {
            if (idProductoSeleccionado == 0) {
                productoDAO.insertar(p);
                UiUtils.mostrarMensajeExito(this, "Producto registrado con éxito.");
            } else {
                productoDAO.actualizar(p);
                UiUtils.mostrarMensajeExito(this, "Producto actualizado con éxito.");
            }
            limpiarFormProducto();
            cargarProductos();
        } catch (Exception e) {
            UiUtils.mostrarMensajeError(this, e.getMessage());
        }
    }

    private void seleccionarProductoTabla() {
        int f = tablaProductos.getSelectedRow();
        if (f < 0) return;
        idProductoSeleccionado = (int) tablaProductos.getValueAt(f, 0);
        txtProdSku.setText(String.valueOf(tablaProductos.getValueAt(f, 1)));
        txtProdNombre.setText(String.valueOf(tablaProductos.getValueAt(f, 2)));
        cmbProdUnidad.setSelectedItem(tablaProductos.getValueAt(f, 3));
        chkProdActivo.setSelected("SÍ".equalsIgnoreCase(String.valueOf(tablaProductos.getValueAt(f, 7))));
    }

    private void limpiarFormProducto() {
        idProductoSeleccionado = 0;
        txtProdSku.setText("");
        txtProdNombre.setText("");
        chkProdActivo.setSelected(true);
    }

    // PROVEEDORES LOGIC
    private void cargarProveedores() {
        new SwingWorker<List<Proveedor>, Void>() {
            @Override
            protected List<Proveedor> doInBackground() throws Exception {
                return proveedorDAO.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    List<Proveedor> lista = get();
                    String[] cols = {"ID", "RFC", "Razón Social / Nombre", "Teléfono", "Activo"};
                    DefaultTableModel m = new DefaultTableModel(cols, 0) {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    if (lista != null) {
                        for (Proveedor pr : lista) {
                            m.addRow(new Object[]{pr.getIdProveedor(), pr.getRfc(), pr.getNombre(), pr.getTelefono(), pr.isActivo() ? "SÍ" : "NO"});
                        }
                    }
                    tablaProveedores.setModel(m);
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void guardarProveedor() {
        String rfc = txtProvRfc.getText().trim();
        String nom = txtProvNombre.getText().trim();
        if (rfc.length() < 12 || rfc.length() > 13) {
            UiUtils.mostrarMensajeAdvertencia(this, "El RFC debe tener exactamente 12 o 13 caracteres.");
            return;
        }
        if (nom.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "El nombre del proveedor es obligatorio.");
            return;
        }

        Proveedor p = new Proveedor(idProveedorSeleccionado, rfc, nom, txtProvTel.getText().trim(), chkProvActivo.isSelected());
        try {
            if (idProveedorSeleccionado == 0) {
                proveedorDAO.insertar(p);
                UiUtils.mostrarMensajeExito(this, "Proveedor guardado con éxito.");
            } else {
                proveedorDAO.actualizar(p);
                UiUtils.mostrarMensajeExito(this, "Proveedor actualizado con éxito.");
            }
            limpiarFormProveedor();
            cargarProveedores();
        } catch (Exception e) {
            UiUtils.mostrarMensajeError(this, e.getMessage());
        }
    }

    private void seleccionarProveedorTabla() {
        int f = tablaProveedores.getSelectedRow();
        if (f < 0) return;
        idProveedorSeleccionado = (int) tablaProveedores.getValueAt(f, 0);
        txtProvRfc.setText(String.valueOf(tablaProveedores.getValueAt(f, 1)));
        txtProvNombre.setText(String.valueOf(tablaProveedores.getValueAt(f, 2)));
        txtProvTel.setText(String.valueOf(tablaProveedores.getValueAt(f, 3)));
        chkProvActivo.setSelected("SÍ".equalsIgnoreCase(String.valueOf(tablaProveedores.getValueAt(f, 4))));
    }

    private void limpiarFormProveedor() {
        idProveedorSeleccionado = 0;
        txtProvRfc.setText("");
        txtProvNombre.setText("");
        txtProvTel.setText("");
        chkProvActivo.setSelected(true);
    }

    // CLIENTES LOGIC
    private void cargarClientes() {
        new SwingWorker<List<Cliente>, Void>() {
            @Override
            protected List<Cliente> doInBackground() throws Exception {
                return clienteDAO.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    List<Cliente> lista = get();
                    String[] cols = {"ID", "RFC", "Nombre", "Teléfono", "Correo", "ID Usuario", "Activo"};
                    DefaultTableModel m = new DefaultTableModel(cols, 0) {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    if (lista != null) {
                        for (Cliente c : lista) {
                            m.addRow(new Object[]{c.getIdCliente(), c.getRfc(), c.getNombre(), c.getTelefono(), c.getCorreo(), c.getIdUsuario(), c.isActivo() ? "SÍ" : "NO"});
                        }
                    }
                    tablaClientes.setModel(m);
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void guardarCliente() {
        String rfc = txtCliRfc.getText().trim();
        String nom = txtCliNombre.getText().trim();
        if (rfc.length() < 12 || rfc.length() > 13) {
            UiUtils.mostrarMensajeAdvertencia(this, "El RFC debe tener exactamente 12 o 13 caracteres.");
            return;
        }
        if (nom.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "El nombre del cliente es obligatorio.");
            return;
        }

        Usuario u = (Usuario) cmbCliUsuario.getSelectedItem();
        Integer idUsr = u != null ? u.getIdUsuario() : null;

        Cliente c = new Cliente(idClienteSeleccionado, idUsr, rfc, nom, txtCliTel.getText().trim(), txtCliCorreo.getText().trim(), chkCliActivo.isSelected());
        try {
            if (idClienteSeleccionado == 0) {
                clienteDAO.insertar(c);
                UiUtils.mostrarMensajeExito(this, "Cliente guardado con éxito.");
            } else {
                clienteDAO.actualizar(c);
                UiUtils.mostrarMensajeExito(this, "Cliente actualizado con éxito.");
            }
            limpiarFormCliente();
            cargarClientes();
        } catch (Exception e) {
            UiUtils.mostrarMensajeError(this, e.getMessage());
        }
    }

    private void seleccionarClienteTabla() {
        int f = tablaClientes.getSelectedRow();
        if (f < 0) return;
        idClienteSeleccionado = (int) tablaClientes.getValueAt(f, 0);
        txtCliRfc.setText(String.valueOf(tablaClientes.getValueAt(f, 1)));
        txtCliNombre.setText(String.valueOf(tablaClientes.getValueAt(f, 2)));
        txtCliTel.setText(String.valueOf(tablaClientes.getValueAt(f, 3)));
        txtCliCorreo.setText(String.valueOf(tablaClientes.getValueAt(f, 4)));
        chkCliActivo.setSelected("SÍ".equalsIgnoreCase(String.valueOf(tablaClientes.getValueAt(f, 6))));
    }

    private void limpiarFormCliente() {
        idClienteSeleccionado = 0;
        txtCliRfc.setText("");
        txtCliNombre.setText("");
        txtCliTel.setText("");
        txtCliCorreo.setText("");
        chkCliActivo.setSelected(true);
    }

    // USUARIOS LOGIC
    private void cargarUsuarios() {
        new SwingWorker<List<Usuario>, Void>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                return usuarioDAO.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    List<Usuario> lista = get();
                    cmbCliUsuario.removeAllItems();
                    String[] cols = {"ID", "Username", "Nombre Completo", "Rol", "Activo", "Fecha Creación"};
                    DefaultTableModel m = new DefaultTableModel(cols, 0) {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    if (lista != null) {
                        for (Usuario u : lista) {
                            m.addRow(new Object[]{u.getIdUsuario(), u.getUsername(), u.getNombreCompleto(), u.getRol(), u.isActivo() ? "SÍ" : "NO", u.getFechaCreacion()});
                            if (u.getRol() == RolUsuario.CLIENTE) {
                                cmbCliUsuario.addItem(u);
                            }
                        }
                    }
                    tablaUsuarios.setModel(m);
                } catch (Exception e) {}
            }
        }.execute();
    }

    private void guardarUsuario() {
        String username = txtUsrUsername.getText().trim();
        String nom = txtUsrNombre.getText().trim();
        if (username.isEmpty() || nom.isEmpty()) {
            UiUtils.mostrarMensajeAdvertencia(this, "Username y Nombre Completo son obligatorios.");
            return;
        }

        String pass = new String(txtUsrPass.getPassword()).trim();
        RolUsuario rol = (RolUsuario) cmbUsrRol.getSelectedItem();

        try {
            if (idUsuarioSeleccionado == 0) {
                if (pass.isEmpty()) {
                    UiUtils.mostrarMensajeAdvertencia(this, "Debe especificar una contraseña para el nuevo usuario.");
                    return;
                }
                Usuario u = new Usuario();
                u.setUsername(username);
                u.setNombreCompleto(nom);
                u.setRol(rol);
                u.setPasswordHash(AuthService.hashSha256(pass));
                u.setActivo(chkUsrActivo.isSelected());
                usuarioDAO.insertar(u);
                UiUtils.mostrarMensajeExito(this, "Usuario creado exitosamente.");
            } else {
                Usuario u = usuarioDAO.findById(idUsuarioSeleccionado);
                u.setNombreCompleto(nom);
                u.setRol(rol);
                u.setActivo(chkUsrActivo.isSelected());
                boolean actualizaPass = !pass.isEmpty();
                if (actualizaPass) {
                    u.setPasswordHash(AuthService.hashSha256(pass));
                }
                usuarioDAO.actualizar(u, actualizaPass);
                UiUtils.mostrarMensajeExito(this, "Usuario actualizado exitosamente.");
            }
            limpiarFormUsuario();
            cargarUsuarios();
        } catch (Exception e) {
            UiUtils.mostrarMensajeError(this, e.getMessage());
        }
    }

    private void seleccionarUsuarioTabla() {
        int f = tablaUsuarios.getSelectedRow();
        if (f < 0) return;
        idUsuarioSeleccionado = (int) tablaUsuarios.getValueAt(f, 0);
        txtUsrUsername.setText(String.valueOf(tablaUsuarios.getValueAt(f, 1)));
        txtUsrNombre.setText(String.valueOf(tablaUsuarios.getValueAt(f, 2)));
        cmbUsrRol.setSelectedItem(tablaUsuarios.getValueAt(f, 3));
        chkUsrActivo.setSelected("SÍ".equalsIgnoreCase(String.valueOf(tablaUsuarios.getValueAt(f, 4))));
        txtUsrPass.setText("");
    }

    private void limpiarFormUsuario() {
        idUsuarioSeleccionado = 0;
        txtUsrUsername.setText("");
        txtUsrNombre.setText("");
        txtUsrPass.setText("");
        chkUsrActivo.setSelected(true);
    }
}
