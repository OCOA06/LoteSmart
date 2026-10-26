package com.lotesmart.vista;

import com.lotesmart.datos.Conexion;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.Usuario;
import com.lotesmart.negocio.AuthService;
import com.lotesmart.negocio.ReglaNegocioException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class LoginFrame extends JFrame {

    private final AuthService authService;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblEstadoConexion;

    public LoginFrame() {
        this.authService = new AuthService();
        initComponents();
    }

    private void initComponents() {
        setTitle("LoteSmart Enterprise - Inicio de Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        mainPanel.setBackground(Color.WHITE);

        // Header institucional
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("LOTESMART ENTERPRISE");
        lblLogo.setFont(UiUtils.FONT_TITULO);
        lblLogo.setForeground(UiUtils.COLOR_PRIMARIO);
        lblLogo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Control de Caducidades FEFO & Trazabilidad Sanitaria");
        lblSub.setFont(UiUtils.FONT_REGULAR);
        lblSub.setForeground(UiUtils.COLOR_SECUNDARIO);
        lblSub.setAlignmentX(CENTER_ALIGNMENT);

        headerPanel.add(lblLogo);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(lblSub);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Formulario central
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JPanel formCard = new JPanel(new GridLayout(4, 1, 6, 6));
        UiUtils.aplicarEstiloCard(formCard);

        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setFont(UiUtils.FONT_BOLD);
        txtUsername = new JTextField("admin");
        txtUsername.setPreferredSize(new Dimension(200, 32));

        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setFont(UiUtils.FONT_BOLD);
        txtPassword = new JPasswordField("admin123");
        txtPassword.setPreferredSize(new Dimension(200, 32));

        formCard.add(lblUser);
        formCard.add(txtUsername);
        formCard.add(lblPass);
        formCard.add(txtPassword);

        centerPanel.add(formCard);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(UiUtils.FONT_BOLD);
        btnLogin.setBackground(UiUtils.COLOR_PRIMARIO);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setPreferredSize(new Dimension(420, 38));
        btnLogin.setMaximumSize(new Dimension(420, 38));
        btnLogin.setAlignmentX(CENTER_ALIGNMENT);
        btnLogin.addActionListener(e -> ejecutarLogin());
        getRootPane().setDefaultButton(btnLogin);

        centerPanel.add(btnLogin);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Botones de acceso rápido para evaluación de roles
        JPanel panelDemo = new JPanel(new GridLayout(2, 2, 6, 6));
        panelDemo.setBorder(BorderFactory.createTitledBorder("Acceso Rápido para Demostración (3 Roles)"));
        panelDemo.setOpaque(false);

        JButton btnDemoAdmin = new JButton("Admin (admin123)");
        btnDemoAdmin.addActionListener(e -> setDemoCredentials("admin", "admin123"));

        JButton btnDemoOperador = new JButton("Operador (operador123)");
        btnDemoOperador.addActionListener(e -> setDemoCredentials("operador", "operador123"));

        JButton btnDemoCliente1 = new JButton("Esperanza (cliente123)");
        btnDemoCliente1.addActionListener(e -> setDemoCredentials("esperanza", "cliente123"));

        JButton btnDemoCliente2 = new JButton("San Ángel (cliente123)");
        btnDemoCliente2.addActionListener(e -> setDemoCredentials("sanangel", "cliente123"));

        panelDemo.add(btnDemoAdmin);
        panelDemo.add(btnDemoOperador);
        panelDemo.add(btnDemoCliente1);
        panelDemo.add(btnDemoCliente2);

        centerPanel.add(panelDemo);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Footer con estado de conexión y configuración BD
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        lblEstadoConexion = new JLabel("Conexión: Supabase Cloud PostgreSQL");
        lblEstadoConexion.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 11));
        lblEstadoConexion.setForeground(new Color(100, 110, 120));

        JButton btnConfigBd = new JButton("Configurar BD");
        btnConfigBd.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        btnConfigBd.addActionListener(e -> abrirConfiguracionBd());

        footerPanel.add(lblEstadoConexion, BorderLayout.WEST);
        footerPanel.add(btnConfigBd, BorderLayout.EAST);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void setDemoCredentials(String u, String p) {
        txtUsername.setText(u);
        txtPassword.setText(p);
    }

    private void ejecutarLogin() {
        String u = txtUsername.getText().trim();
        String p = new String(txtPassword.getPassword()).trim();

        btnLogin.setEnabled(false);
        lblEstadoConexion.setText("Autenticando usuario...");

        new SwingWorker<Usuario, Void>() {
            private Exception error;

            @Override
            protected Usuario doInBackground() {
                try {
                    return authService.autenticar(u, p);
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                lblEstadoConexion.setText("Conexión: Supabase Cloud PostgreSQL");
                if (error != null) {
                    UiUtils.mostrarMensajeError(LoginFrame.this, "Error de Inicio de Sesión:\n" + error.getMessage());
                } else {
                    try {
                        Usuario usuarioAutenticado = get();
                        if (usuarioAutenticado != null) {
                            dispose(); // Cerrar login
                            VentanaPrincipal ventanaPrincipal = new VentanaPrincipal();
                            ventanaPrincipal.setVisible(true);
                        }
                    } catch (Exception e) {
                        UiUtils.mostrarMensajeError(LoginFrame.this, e.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void abrirConfiguracionBd() {
        Properties props = new Properties();
        File f = new File("db.properties");
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (Exception ignored) {}
        }

        JTextField txtUrl = new JTextField(props.getProperty("db.url", Conexion.DEFAULT_URL), 30);
        JTextField txtUser = new JTextField(props.getProperty("db.user", Conexion.DEFAULT_USER), 30);
        JPasswordField txtPass = new JPasswordField(props.getProperty("db.password", Conexion.DEFAULT_PASSWORD), 30);

        JPanel p = new JPanel(new GridLayout(6, 1, 5, 5));
        p.add(new JLabel("URL JDBC (Supabase):"));
        p.add(txtUrl);
        p.add(new JLabel("Usuario (ej. postgres):"));
        p.add(txtUser);
        p.add(new JLabel("Contraseña:"));
        p.add(txtPass);

        int opt = JOptionPane.showConfirmDialog(this, p, "Configuración de Conexión a Base de Datos", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            props.setProperty("db.url", txtUrl.getText().trim());
            props.setProperty("db.user", txtUser.getText().trim());
            props.setProperty("db.password", new String(txtPass.getPassword()).trim());

            try (FileOutputStream out = new FileOutputStream(f)) {
                props.store(out, "Configuracion de conexion LoteSmart");
                Conexion.cerrarPool(); // Reiniciar pool
                UiUtils.mostrarMensajeExito(this, "Configuración guardada en db.properties exitosamente.");
            } catch (Exception ex) {
                UiUtils.mostrarMensajeError(this, "No se pudo guardar la configuración: " + ex.getMessage());
            }
        }
    }
}
