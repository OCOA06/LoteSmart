package com.lotesmart.datos;

import com.lotesmart.modelo.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    public Proveedor findById(int idProveedor) throws SQLException {
        String sql = "SELECT id_proveedor, rfc, nombre, telefono, activo " +
                     "FROM proveedor WHERE id_proveedor = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProveedor(rs);
                }
            }
        }
        return null;
    }

    public List<Proveedor> listarTodos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id_proveedor, rfc, nombre, telefono, activo " +
                     "FROM proveedor ORDER BY nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearProveedor(rs));
            }
        }
        return lista;
    }

    public List<Proveedor> listarActivos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id_proveedor, rfc, nombre, telefono, activo " +
                     "FROM proveedor WHERE activo = TRUE ORDER BY nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearProveedor(rs));
            }
        }
        return lista;
    }

    public int insertar(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedor (rfc, nombre, telefono, activo) " +
                     "VALUES (?, ?, ?, ?) RETURNING id_proveedor";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getRfc().trim().toUpperCase());
            ps.setString(2, p.getNombre().trim());
            ps.setString(3, p.getTelefono() != null ? p.getTelefono().trim() : null);
            ps.setBoolean(4, p.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setIdProveedor(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE proveedor SET rfc = ?, nombre = ?, telefono = ?, activo = ? " +
                     "WHERE id_proveedor = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getRfc().trim().toUpperCase());
            ps.setString(2, p.getNombre().trim());
            ps.setString(3, p.getTelefono() != null ? p.getTelefono().trim() : null);
            ps.setBoolean(4, p.isActivo());
            ps.setInt(5, p.getIdProveedor());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idProveedor, boolean activo) throws SQLException {
        String sql = "UPDATE proveedor SET activo = ? WHERE id_proveedor = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idProveedor);
            return ps.executeUpdate() > 0;
        }
    }

    private Proveedor mapearProveedor(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("id_proveedor"));
        p.setRfc(rs.getString("rfc"));
        p.setNombre(rs.getString("nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    }
}
