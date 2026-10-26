package com.lotesmart.vista;

import com.lotesmart.modelo.RolUsuario;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.Usuario;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class VentanaPrincipal extends JFrame {

    private JTabbedPane tabbedPane;
    private JLabel lblUsuarioSesion;

    public VentanaPrincipal() {
        initComponents();
    }

    private void initComponents() {
        setTitle("LoteSmart Enterprise - Sistema de Control de Inventario por Lotes FEFO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // 1. BARRA SUPERIOR INSTITUCIONAL
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UiUtils.COLOR_PRIMARIO);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Título del sistema
        JLabel lblTitulo = new JLabel("LOTESMART ENTERPRISE  |  Despacho FEFO & Trazabilidad");
        lblTitulo.setFont(UiUtils.FONT_TITULO);
        lblTitulo.setForeground(Color.WHITE);
        topBar.add(lblTitulo, BorderLayout.WEST);

        // Perfil de usuario y botón cerrar sesión
        JPanel pUsuario = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pUsuario.setOpaque(false);

        Usuario user = SesionActual.getInstance().getUsuario();
        String infoUser = (user != null ? user.getNombreCompleto() + " (" + user.getRol().getDescripcion() + ")" : "Usuario Anónimo");
        lblUsuarioSesion = new JLabel("Sesión activa: " + infoUser);
        lblUsuarioSesion.setFont(UiUtils.FONT_BOLD);
        lblUsuarioSesion.setForeground(Color.WHITE);

        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        btnCerrarSesion.setFont(UiUtils.FONT_BOLD);
        btnCerrarSesion.setBackground(new Color(220, 53, 69));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.addActionListener(e -> cerrarSesion());

        pUsuario.add(lblUsuarioSesion);
        pUsuario.add(btnCerrarSesion);
        topBar.add(pUsuario, BorderLayout.EAST);

        mainPanel.add(topBar, BorderLayout.NORTH);

        // 2. MÓDULOS SEGÚN ROL (RBAC)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiUtils.FONT_BOLD);

        configurarModulosPorRol();

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private void configurarModulosPorRol() {
        RolUsuario rol = SesionActual.getInstance().getRol();
        if (rol == null) {
            return;
        }

        switch (rol) {
            case ADMIN:
                // Acceso total a las 6 pestañas empresariales
                tabbedPane.addTab("  Tablero de Control  ", new TableroPanel());
                tabbedPane.addTab("  Punto de Venta FEFO  ", new PuntoVentaPanel());
                tabbedPane.addTab("  Gestión de Lotes y Almacén  ", new GestionLotesPanel());
                tabbedPane.addTab("  Catálogos  ", new CatalogosPanel());
                tabbedPane.addTab("  Trazabilidad y Auditoría  ", new TrazabilidadAuditoriaPanel());
                tabbedPane.addTab("  Centro de Reportes  ", new CentroReportesPanel());
                break;

            case OPERADOR:
                // Acceso operativo restringido (sin cancelación de ventas ni auditoría de sistema ni márgenes)
                tabbedPane.addTab("  Tablero de Control  ", new TableroPanel());
                tabbedPane.addTab("  Punto de Venta FEFO  ", new PuntoVentaPanel());
                tabbedPane.addTab("  Gestión de Lotes y Almacén  ", new GestionLotesPanel());
                tabbedPane.addTab("  Centro de Reportes  ", new CentroReportesPanel());
                break;

            case CLIENTE:
                // Portal B2B simplificado y guiado de autopedido y trazabilidad
                tabbedPane.addTab("  Portal B2B de Autopedido  ", new PortalClientePanel());
                break;
        }
    }

    private void cerrarSesion() {
        if (UiUtils.confirmar(this, "¿Está seguro de que desea cerrar la sesión actual?", "Cerrar Sesión")) {
            SesionActual.getInstance().cerrarSesion();
            dispose();
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        }
    }
}
