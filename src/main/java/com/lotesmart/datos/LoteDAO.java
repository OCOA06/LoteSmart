package com.lotesmart.datos;

import com.lotesmart.modelo.EstadoLote;
import com.lotesmart.modelo.Lote;
import com.lotesmart.modelo.LoteDisponible;
import com.lotesmart.modelo.Movimiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoteDAO {

    public Lote findById(int idLote) throws SQLException {
        String sql = "SELECT id_lote, id_producto, sku, nombre_producto, unidad, dias_minimos_venta, " +
                     "id_proveedor, nombre_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, " +
                     "cantidad_disponible, costo_unitario, valor_existencia, estado, motivo_bloqueo, " +
                     "dias_restantes, semaforo " +
                     "FROM v_lote_detalle WHERE id_lote = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLote);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearLoteDetalle(rs);
                }
            }
        }
        return null;
    }

    public List<Lote> listarTodos() throws SQLException {
        List<Lote> lista = new ArrayList<>();
        String sql = "SELECT id_lote, id_producto, sku, nombre_producto, unidad, dias_minimos_venta, " +
                     "id_proveedor, nombre_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, " +
                     "cantidad_disponible, costo_unitario, valor_existencia, estado, motivo_bloqueo, " +
                     "dias_restantes, semaforo " +
                     "FROM v_lote_detalle " +
                     "ORDER BY fecha_caducidad ASC, id_lote ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearLoteDetalle(rs));
            }
        }
        return lista;
    }

    public List<Lote> listarPorProducto(int idProducto) throws SQLException {
        List<Lote> lista = new ArrayList<>();
        String sql = "SELECT id_lote, id_producto, sku, nombre_producto, unidad, dias_minimos_venta, " +
                     "id_proveedor, nombre_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, " +
                     "cantidad_disponible, costo_unitario, valor_existencia, estado, motivo_bloqueo, " +
                     "dias_restantes, semaforo " +
                     "FROM v_lote_detalle " +
                     "WHERE id_producto = ? " +
                     "ORDER BY fecha_caducidad ASC, id_lote ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLoteDetalle(rs));
                }
            }
        }
        return lista;
    }

    public List<LoteDisponible> listarDisponiblesPorProducto(int idProducto) throws SQLException {
        try (Connection conn = Conexion.getConexion()) {
            return listarDisponiblesPorProducto(conn, idProducto, false);
        }
    }

    public List<LoteDisponible> listarDisponiblesPorProductoForUpdate(Connection conn, int idProducto) throws SQLException {
        return listarDisponiblesPorProducto(conn, idProducto, true);
    }

    private List<LoteDisponible> listarDisponiblesPorProducto(Connection conn, int idProducto, boolean forUpdate) throws SQLException {
        List<LoteDisponible> lista = new ArrayList<>();
        String sql = "SELECT id_lote, id_producto, codigo_lote, fecha_caducidad, cantidad_disponible, costo_unitario " +
                     "FROM lote " +
                     "WHERE id_producto = ? AND estado = 'VIGENTE' AND cantidad_disponible > 0 " +
                     "ORDER BY fecha_caducidad ASC, id_lote ASC" +
                     (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoteDisponible ld = new LoteDisponible();
                    ld.setIdLote(rs.getInt("id_lote"));
                    ld.setIdProducto(rs.getInt("id_producto"));
                    ld.setCodigoLote(rs.getString("codigo_lote"));
                    Date d = rs.getDate("fecha_caducidad");
                    ld.setFechaCaducidad(d != null ? d.toLocalDate() : null);
                    ld.setExistencia(rs.getInt("cantidad_disponible"));
                    ld.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
                    lista.add(ld);
                }
            }
        }
        return lista;
    }

    public int insertar(Lote l) throws SQLException {
        String sql = "INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, " +
                     "cantidad_inicial, cantidad_disponible, costo_unitario, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_lote";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, l.getIdProducto());
            ps.setInt(2, l.getIdProveedor());
            ps.setString(3, l.getCodigoLote().trim().toUpperCase());
            ps.setDate(4, Date.valueOf(l.getFechaCaducidad()));
            ps.setInt(5, l.getCantidadInicial());
            ps.setInt(6, l.getCantidadInicial()); // Inicialmente disponible = inicial
            ps.setBigDecimal(7, l.getCostoUnitario());
            ps.setString(8, EstadoLote.VIGENTE.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    l.setIdLote(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean actualizarBloqueo(int idLote, boolean bloquear, String motivo) throws SQLException {
        String sql;
        if (bloquear) {
            sql = "UPDATE lote SET estado = 'BLOQUEADO', motivo_bloqueo = ? WHERE id_lote = ?";
            try (Connection conn = Conexion.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, motivo != null ? motivo.trim() : "Bloqueo sanitario preventivo");
                ps.setInt(2, idLote);
                return ps.executeUpdate() > 0;
            }
        } else {
            // Desbloqueo: recalcular estado según existencia y fecha
            sql = "UPDATE lote SET motivo_bloqueo = NULL, " +
                  "estado = CASE " +
                  "   WHEN cantidad_disponible = 0 THEN 'AGOTADO' " +
                  "   WHEN fecha_caducidad < CURRENT_DATE THEN 'CADUCADO' " +
                  "   ELSE 'VIGENTE' " +
                  "END " +
                  "WHERE id_lote = ?";
            try (Connection conn = Conexion.getConexion();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idLote);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean actualizarDatosIniciales(int idLote, LocalDate nuevaFecha, BigDecimal nuevoCosto, int nuevaCantidadInicial) throws SQLException {
        // Solo permitido por trigger de BD si no hay movimientos
        String sql = "UPDATE lote SET fecha_caducidad = ?, costo_unitario = ?, cantidad_inicial = ?, " +
                     "cantidad_disponible = ? WHERE id_lote = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(nuevaFecha));
            ps.setBigDecimal(2, nuevoCosto);
            ps.setInt(3, nuevaCantidadInicial);
            ps.setInt(4, nuevaCantidadInicial);
            ps.setInt(5, idLote);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean tieneMovimientos(int idLote) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM movimiento WHERE id_lote = ?)";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLote);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    public boolean eliminar(int idLote) throws SQLException {
        String sql = "DELETE FROM lote WHERE id_lote = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLote);
            return ps.executeUpdate() > 0;
        }
    }

    public int registrarMovimiento(Connection conn, Movimiento mov) throws SQLException {
        String sql = "INSERT INTO movimiento (id_lote, id_venta, tipo, cantidad, precio_unitario, motivo, usuario) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_movimiento";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mov.getIdLote());
            if (mov.getIdVenta() != null && mov.getIdVenta() > 0) {
                ps.setInt(2, mov.getIdVenta());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, mov.getTipo().name());
            ps.setInt(4, mov.getCantidad());
            if (mov.getPrecioUnitario() != null) {
                ps.setBigDecimal(5, mov.getPrecioUnitario());
            } else {
                ps.setNull(5, Types.NUMERIC);
            }
            ps.setString(6, mov.getMotivo() != null ? mov.getMotivo().trim() : null);
            ps.setString(7, mov.getUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idMov = rs.getInt(1);
                    mov.setIdMovimiento(idMov);
                    return idMov;
                }
            }
        }
        return 0;
    }

    private Lote mapearLoteDetalle(ResultSet rs) throws SQLException {
        Lote l = new Lote();
        l.setIdLote(rs.getInt("id_lote"));
        l.setIdProducto(rs.getInt("id_producto"));
        l.setSkuProducto(rs.getString("sku"));
        l.setNombreProducto(rs.getString("nombre_producto"));
        l.setUnidadProducto(rs.getString("unidad"));
        l.setDiasMinimosVenta(rs.getInt("dias_minimos_venta"));
        l.setIdProveedor(rs.getInt("id_proveedor"));
        l.setNombreProveedor(rs.getString("nombre_proveedor"));
        l.setCodigoLote(rs.getString("codigo_lote"));
        Date d = rs.getDate("fecha_caducidad");
        if (d != null) {
            l.setFechaCaducidad(d.toLocalDate());
        }
        l.setCantidadInicial(rs.getInt("cantidad_inicial"));
        l.setCantidadDisponible(rs.getInt("cantidad_disponible"));
        l.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
        l.setValorExistencia(rs.getBigDecimal("valor_existencia"));
        l.setEstado(EstadoLote.fromString(rs.getString("estado")));
        l.setMotivoBloqueo(rs.getString("motivo_bloqueo"));
        l.setDiasRestantes(rs.getInt("dias_restantes"));
        l.setSemaforo(rs.getString("semaforo"));
        return l;
    }
}
