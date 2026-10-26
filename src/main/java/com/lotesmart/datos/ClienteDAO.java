package com.lotesmart.datos;

import com.lotesmart.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public Cliente findById(int idCliente) throws SQLException {
        String sql = "SELECT id_cliente, id_usuario, rfc, nombre, telefono, correo, activo " +
                     "FROM cliente WHERE id_cliente = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        }
        return null;
    }

    public Cliente findByUsuarioId(int idUsuario) throws SQLException {
        String sql = "SELECT id_cliente, id_usuario, rfc, nombre, telefono, correo, activo " +
                     "FROM cliente WHERE id_usuario = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        }
        return null;
    }

    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, id_usuario, rfc, nombre, telefono, correo, activo " +
                     "FROM cliente ORDER BY nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCliente(rs));
            }
        }
        return lista;
    }

    public List<Cliente> listarActivos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, id_usuario, rfc, nombre, telefono, correo, activo " +
                     "FROM cliente WHERE activo = TRUE ORDER BY nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCliente(rs));
            }
        }
        return lista;
    }

    public int insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (id_usuario, rfc, nombre, telefono, correo, activo) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_cliente";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (c.getIdUsuario() != null && c.getIdUsuario() > 0) {
                ps.setInt(1, c.getIdUsuario());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, c.getRfc().trim().toUpperCase());
            ps.setString(3, c.getNombre().trim());
            ps.setString(4, c.getTelefono() != null ? c.getTelefono().trim() : null);
            ps.setString(5, c.getCorreo() != null ? c.getCorreo().trim() : null);
            ps.setBoolean(6, c.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    c.setIdCliente(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean actualizar(Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET id_usuario = ?, rfc = ?, nombre = ?, telefono = ?, correo = ?, activo = ? " +
                     "WHERE id_cliente = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (c.getIdUsuario() != null && c.getIdUsuario() > 0) {
                ps.setInt(1, c.getIdUsuario());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, c.getRfc().trim().toUpperCase());
            ps.setString(3, c.getNombre().trim());
            ps.setString(4, c.getTelefono() != null ? c.getTelefono().trim() : null);
            ps.setString(5, c.getCorreo() != null ? c.getCorreo().trim() : null);
            ps.setBoolean(6, c.isActivo());
            ps.setInt(7, c.getIdCliente());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idCliente, boolean activo) throws SQLException {
        String sql = "UPDATE cliente SET activo = ? WHERE id_cliente = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idCliente);
            return ps.executeUpdate() > 0;
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setIdCliente(rs.getInt("id_cliente"));
        int idUsr = rs.getInt("id_usuario");
        if (!rs.wasNull()) {
            c.setIdUsuario(idUsr);
        }
        c.setRfc(rs.getString("rfc"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        c.setActivo(rs.getBoolean("activo"));
        return c;
    }
}
