package com.lotesmart.datos;

import com.lotesmart.modelo.RolUsuario;
import com.lotesmart.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Usuario findByUsername(String username) throws SQLException {
        String sql = "SELECT id_usuario, username, password_hash, nombre_completo, rol, activo, fecha_creacion " +
                     "FROM usuario WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public Usuario findById(int idUsuario) throws SQLException {
        String sql = "SELECT id_usuario, username, password_hash, nombre_completo, rol, activo, fecha_creacion " +
                     "FROM usuario WHERE id_usuario = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, username, password_hash, nombre_completo, rol, activo, fecha_creacion " +
                     "FROM usuario ORDER BY id_usuario ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        }
        return lista;
    }

    public int insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (username, password_hash, nombre_completo, rol, activo) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id_usuario";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername().trim());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNombreCompleto().trim());
            ps.setString(4, u.getRol().name());
            ps.setBoolean(5, u.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    u.setIdUsuario(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean actualizar(Usuario u, boolean actualizarPassword) throws SQLException {
        String sql;
        if (actualizarPassword) {
            sql = "UPDATE usuario SET nombre_completo = ?, rol = ?, activo = ?, password_hash = ? WHERE id_usuario = ?";
        } else {
            sql = "UPDATE usuario SET nombre_completo = ?, rol = ?, activo = ? WHERE id_usuario = ?";
        }
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombreCompleto().trim());
            ps.setString(2, u.getRol().name());
            ps.setBoolean(3, u.isActivo());
            if (actualizarPassword) {
                ps.setString(4, u.getPasswordHash());
                ps.setInt(5, u.getIdUsuario());
            } else {
                ps.setInt(4, u.getIdUsuario());
            }
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idUsuario, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET activo = ? WHERE id_usuario = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setRol(RolUsuario.fromString(rs.getString("rol")));
        u.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) {
            u.setFechaCreacion(ts.toLocalDateTime());
        }
        return u;
    }
}
