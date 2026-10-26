package com.lotesmart;

import com.lotesmart.datos.Conexion;
import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.vista.LoginFrame;
import com.lotesmart.vista.UiUtils;

import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Inicializar tema moderno FlatLaf
        UiUtils.inicializarTema();

        // 2. Ejecutar procedimiento almacenado de marcado de caducados al arranque
        new Thread(() -> {
            try {
                LOGGER.info("Verificando caducidades automáticas con sp_marcar_caducados()...");
                ConsultaDAO consultaDAO = new ConsultaDAO();
                consultaDAO.ejecutarSpMarcarCaducados();
                LOGGER.info("Marcado automático de caducidades completado.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Aviso al conectar con base de datos al inicio: " + e.getMessage());
            }
        }).start();

        // 3. Abrir ventana de Login en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
