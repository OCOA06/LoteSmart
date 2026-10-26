package com.lotesmart.datos;

import com.lotesmart.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public Producto findById(int idProducto) throws SQLException {
        String sql = "SELECT p.id_producto, p.sku, p.nombre, p.unidad, p.precio_venta, p.stock_minimo, p.dias_minimos_venta, p.activo, " +
                     "s.stock_vigente, s.stock_no_vendible, s.stock_total, s.alerta_reorden " +
                     "FROM producto p " +
                     "LEFT JOIN v_stock_producto s ON p.id_producto = s.id_producto " +
                     "WHERE p.id_producto = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProductoConStock(rs);
                }
            }
        }
        return null;
    }

    public Producto findBySku(String sku) throws SQLException {
        String sql = "SELECT p.id_producto, p.sku, p.nombre, p.unidad, p.precio_venta, p.stock_minimo, p.dias_minimos_venta, p.activo, " +
                     "s.stock_vigente, s.stock_no_vendible, s.stock_total, s.alerta_reorden " +
                     "FROM producto p " +
                     "LEFT JOIN v_stock_producto s ON p.id_producto = s.id_producto " +
                     "WHERE LOWER(p.sku) = LOWER(?)";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProductoConStock(rs);
                }
            }
        }
        return null;
    }

    public List<Producto> listarTodos() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.sku, p.nombre, p.unidad, p.precio_venta, p.stock_minimo, p.dias_minimos_venta, p.activo, " +
                     "COALESCE(s.stock_vigente, 0) as stock_vigente, COALESCE(s.stock_no_vendible, 0) as stock_no_vendible, " +
                     "COALESCE(s.stock_total, 0) as stock_total, COALESCE(s.alerta_reorden, false) as alerta_reorden " +
                     "FROM producto p " +
                     "LEFT JOIN v_stock_producto s ON p.id_producto = s.id_producto " +
                     "ORDER BY p.nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearProductoConStock(rs));
            }
        }
        return lista;
    }

    public List<Producto> listarActivos() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.id_producto, p.sku, p.nombre, p.unidad, p.precio_venta, p.stock_minimo, p.dias_minimos_venta, p.activo, " +
                     "COALESCE(s.stock_vigente, 0) as stock_vigente, COALESCE(s.stock_no_vendible, 0) as stock_no_vendible, " +
                     "COALESCE(s.stock_total, 0) as stock_total, COALESCE(s.alerta_reorden, false) as alerta_reorden " +
                     "FROM producto p " +
                     "LEFT JOIN v_stock_producto s ON p.id_producto = s.id_producto " +
                     "WHERE p.activo = TRUE " +
                     "ORDER BY p.nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearProductoConStock(rs));
            }
        }
        return lista;
    }

    public int insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (sku, nombre, unidad, precio_venta, stock_minimo, dias_minimos_venta, activo) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_producto";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku().trim().toUpperCase());
            ps.setString(2, p.getNombre().trim());
            ps.setString(3, p.getUnidad().trim().toUpperCase());
            ps.setBigDecimal(4, p.getPrecioVenta());
            ps.setInt(5, p.getStockMinimo());
            ps.setInt(6, p.getDiasMinimosVenta());
            ps.setBoolean(7, p.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setIdProducto(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET sku = ?, nombre = ?, unidad = ?, precio_venta = ?, " +
                     "stock_minimo = ?, dias_minimos_venta = ?, activo = ? WHERE id_producto = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku().trim().toUpperCase());
            ps.setString(2, p.getNombre().trim());
            ps.setString(3, p.getUnidad().trim().toUpperCase());
            ps.setBigDecimal(4, p.getPrecioVenta());
            ps.setInt(5, p.getStockMinimo());
            ps.setInt(6, p.getDiasMinimosVenta());
            ps.setBoolean(7, p.isActivo());
            ps.setInt(8, p.getIdProducto());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idProducto, boolean activo) throws SQLException {
        String sql = "UPDATE producto SET activo = ? WHERE id_producto = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        }
    }

    private Producto mapearProductoConStock(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setSku(rs.getString("sku"));
        p.setNombre(rs.getString("nombre"));
        p.setUnidad(rs.getString("unidad"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setDiasMinimosVenta(rs.getInt("dias_minimos_venta"));
        p.setActivo(rs.getBoolean("activo"));
        
        // Stock enriquecido
        p.setStockVigente(rs.getInt("stock_vigente"));
        p.setStockNoVendible(rs.getInt("stock_no_vendible"));
        p.setStockTotal(rs.getInt("stock_total"));
        p.setAlertaReorden(rs.getBoolean("alerta_reorden"));
        return p;
    }
}
