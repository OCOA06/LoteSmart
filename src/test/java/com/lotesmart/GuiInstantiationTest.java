package com.lotesmart;

import com.lotesmart.modelo.RolUsuario;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.Usuario;
import com.lotesmart.vista.LoginFrame;
import com.lotesmart.vista.UiUtils;
import com.lotesmart.vista.VentanaPrincipal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Pruebas de Instanciación de Interfaz Gráfica Swing")
class GuiInstantiationTest {

    @BeforeAll
    static void initTheme() {
        if (!GraphicsEnvironment.isHeadless()) {
            UiUtils.inicializarTema();
        }
    }

    @Test
    @DisplayName("Debe instanciar LoginFrame sin lanzar excepciones de Swing ni FlatLaf")
    void testInstanciarLoginFrame() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        assertDoesNotThrow(() -> {
            LoginFrame frame = new LoginFrame();
            assertNotNull(frame);
            frame.dispose();
        });
    }

    @Test
    @DisplayName("Debe instanciar VentanaPrincipal para rol ADMIN sin errores")
    void testInstanciarVentanaPrincipalAdmin() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        assertDoesNotThrow(() -> {
            Usuario u = new Usuario(1, "admin", "hash", "Administrador General", RolUsuario.ADMIN, true, LocalDateTime.now());
            SesionActual.getInstance().iniciarSesion(u, null);

            VentanaPrincipal v = new VentanaPrincipal();
            assertNotNull(v);
            v.dispose();

            SesionActual.getInstance().cerrarSesion();
        });
    }
}
