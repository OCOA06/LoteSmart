package com.lotesmart.negocio;

import com.lotesmart.datos.ClienteDAO;
import com.lotesmart.datos.ErroresBD;
import com.lotesmart.datos.UsuarioDAO;
import com.lotesmart.modelo.Cliente;
import com.lotesmart.modelo.RolUsuario;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class AuthService {

    private final UsuarioDAO usuarioDAO;
    private final ClienteDAO clienteDAO;

    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
        this.clienteDAO = new ClienteDAO();
    }

    public AuthService(UsuarioDAO usuarioDAO, ClienteDAO clienteDAO) {
        this.usuarioDAO = usuarioDAO;
        this.clienteDAO = clienteDAO;
    }

    public static String hashSha256(String texto) {
        if (texto == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible en el sistema.", e);
        }
    }

    public Usuario autenticar(String username, String password) throws ReglaNegocioException {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new ReglaNegocioException("Por favor ingrese su usuario y contraseña.");
        }

        try {
            Usuario usuario = usuarioDAO.findByUsername(username.trim());
            if (usuario == null) {
                throw new ReglaNegocioException("Credenciales incorrectas. Verifique usuario y contraseña.");
            }

            if (!usuario.isActivo()) {
                throw new ReglaNegocioException("Esta cuenta de usuario se encuentra inactiva. Contacte al Administrador.");
            }

            String hashIngresado = hashSha256(password.trim());
            if (!hashIngresado.equalsIgnoreCase(usuario.getPasswordHash())) {
                throw new ReglaNegocioException("Credenciales incorrectas. Verifique usuario y contraseña.");
            }

            Cliente clienteVinculado = null;
            if (usuario.getRol() == RolUsuario.CLIENTE) {
                clienteVinculado = clienteDAO.findByUsuarioId(usuario.getIdUsuario());
                if (clienteVinculado == null) {
                    throw new ReglaNegocioException("El usuario con rol CLIENTE no tiene una ficha de cliente vinculada.");
                }
                if (!clienteVinculado.isActivo()) {
                    throw new ReglaNegocioException("La ficha de cliente vinculada está inactiva en el sistema.");
                }
            }

            SesionActual.getInstance().iniciarSesion(usuario, clienteVinculado);
            return usuario;

        } catch (SQLException e) {
            throw new ReglaNegocioException("Error de conexión al autenticar: " + ErroresBD.traducir(e), e);
        }
    }

    public void cerrarSesion() {
        SesionActual.getInstance().cerrarSesion();
    }
}
