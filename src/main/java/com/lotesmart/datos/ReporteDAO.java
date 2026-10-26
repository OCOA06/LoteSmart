package com.lotesmart.datos;

import com.lotesmart.modelo.FiltroReporte;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteDAO {

    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 1. DATOS PARA TICKET / NOTA DE VENTA FEFO
    public Map<String, Object> obtenerDatosTicketVenta(int idVenta) throws SQLException {
        Map<String, Object> resultado = new HashMap<>();

        String sqlVenta = "SELECT v.id_venta, ('V-' || LPAD(v.id_venta::text, 6, '0')) as folio, v.fecha, " +
                          "v.total, v.estado, v.motivo_cancelacion, v.fecha_cancelacion, " +
                          "c.nombre as cliente_nombre, c.rfc as cliente_rfc, c.telefono as cliente_telefono, c.correo as cliente_correo, " +
                          "u.username as vendedor_usuario, u.nombre_completo as vendedor_nombre " +
                          "FROM venta v " +
                          "JOIN cliente c ON v.id_cliente = c.id_cliente " +
                          "LEFT JOIN usuario u ON v.id_usuario = u.id_usuario " +
                          "WHERE v.id_venta = ?";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlVenta)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("id_venta", rs.getInt("id_venta"));
                    resultado.put("folio", rs.getString("folio"));
                    Timestamp ts = rs.getTimestamp("fecha");
                    resultado.put("fecha", ts != null ? ts.toLocalDateTime().format(FECHA_HORA_FMT) : "");
                    resultado.put("total", rs.getBigDecimal("total"));
                    resultado.put("estado", rs.getString("estado"));
                    resultado.put("motivo_cancelacion", rs.getString("motivo_cancelacion"));
                    resultado.put("cliente_nombre", rs.getString("cliente_nombre"));
                    resultado.put("cliente_rfc", rs.getString("cliente_rfc"));
                    resultado.put("cliente_telefono", rs.getString("cliente_telefono"));
                    resultado.put("cliente_correo", rs.getString("cliente_correo"));
                    resultado.put("vendedor_usuario", rs.getString("vendedor_usuario"));
                    resultado.put("vendedor_nombre", rs.getString("vendedor_nombre"));
                }
            }
        }

        // Obtener líneas y desglose exacto de lotes FEFO
        String sqlLineas = "SELECT p.sku, p.nombre as producto_nombre, p.unidad, " +
                           "m.cantidad, m.precio_unitario, (m.cantidad * m.precio_unitario) as subtotal, " +
                           "l.codigo_lote, l.fecha_caducidad " +
                           "FROM movimiento m " +
                           "JOIN lote l ON m.id_lote = l.id_lote " +
                           "JOIN producto p ON l.id_producto = p.id_producto " +
                           "WHERE m.id_venta = ? AND m.tipo = 'VENTA' " +
                           "ORDER BY p.nombre ASC, l.fecha_caducidad ASC";

        List<Map<String, Object>> partidas = new ArrayList<>();
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlLineas)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> partida = new HashMap<>();
                    partida.put("sku", rs.getString("sku"));
                    partida.put("producto", rs.getString("producto_nombre"));
                    partida.put("unidad", rs.getString("unidad"));
                    partida.put("cantidad", rs.getInt("cantidad"));
                    partida.put("precio_unitario", rs.getBigDecimal("precio_unitario"));
                    partida.put("subtotal", rs.getBigDecimal("subtotal"));
                    partida.put("codigo_lote", rs.getString("codigo_lote"));
                    Date fc = rs.getDate("fecha_caducidad");
                    partida.put("fecha_caducidad", fc != null ? fc.toLocalDate().format(FECHA_FMT) : "");
                    partidas.add(partida);
                }
            }
        }
        resultado.put("partidas", partidas);
        return resultado;
    }

    // 2. DATOS PARA ALERTA SANITARIA / RETIRO DE LOTE
    public Map<String, Object> obtenerDatosRetiroLote(String codigoLote) throws SQLException {
        Map<String, Object> resultado = new HashMap<>();

        // Datos del lote
        String sqlLote = "SELECT l.id_lote, l.codigo_lote, l.fecha_caducidad, l.cantidad_inicial, l.cantidad_disponible, " +
                         "l.costo_unitario, l.estado, l.motivo_bloqueo, (l.fecha_caducidad - CURRENT_DATE) as dias_restantes, " +
                         "p.sku, p.nombre as producto_nombre, p.unidad, pr.nombre as proveedor_nombre " +
                         "FROM lote l " +
                         "JOIN producto p ON l.id_producto = p.id_producto " +
                         "JOIN proveedor pr ON l.id_proveedor = pr.id_proveedor " +
                         "WHERE LOWER(l.codigo_lote) = LOWER(?)";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlLote)) {
            ps.setString(1, codigoLote.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("id_lote", rs.getInt("id_lote"));
                    resultado.put("codigo_lote", rs.getString("codigo_lote"));
                    Date fc = rs.getDate("fecha_caducidad");
                    resultado.put("fecha_caducidad", fc != null ? fc.toLocalDate().format(FECHA_FMT) : "");
                    resultado.put("cantidad_inicial", rs.getInt("cantidad_inicial"));
                    resultado.put("cantidad_disponible", rs.getInt("cantidad_disponible"));
                    resultado.put("costo_unitario", rs.getBigDecimal("costo_unitario"));
                    resultado.put("estado", rs.getString("estado"));
                    resultado.put("motivo_bloqueo", rs.getString("motivo_bloqueo"));
                    resultado.put("dias_restantes", rs.getInt("dias_restantes"));
                    resultado.put("sku", rs.getString("sku"));
                    resultado.put("producto_nombre", rs.getString("producto_nombre"));
                    resultado.put("unidad", rs.getString("unidad"));
                    resultado.put("proveedor_nombre", rs.getString("proveedor_nombre"));
                }
            }
        }

        // Clientes afectados y folios de venta
        String sqlClientes = "SELECT t.folio_venta, t.fecha_venta, t.id_cliente, t.nombre_cliente, " +
                             "t.telefono_cliente, t.correo_cliente, t.cantidad_entregada " +
                             "FROM v_trazabilidad_lote t " +
                             "WHERE LOWER(t.codigo_lote) = LOWER(?) " +
                             "ORDER BY t.fecha_venta DESC";

        List<Map<String, Object>> clientes = new ArrayList<>();
        int totalEntregado = 0;
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlClientes)) {
            ps.setString(1, codigoLote.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("folio_venta", rs.getString("folio_venta"));
                    Timestamp fv = rs.getTimestamp("fecha_venta");
                    c.put("fecha_venta", fv != null ? fv.toLocalDateTime().format(FECHA_HORA_FMT) : "");
                    c.put("nombre_cliente", rs.getString("nombre_cliente"));
                    c.put("telefono_cliente", rs.getString("telefono_cliente") != null ? rs.getString("telefono_cliente") : "-");
                    c.put("correo_cliente", rs.getString("correo_cliente") != null ? rs.getString("correo_cliente") : "-");
                    int cant = rs.getInt("cantidad_entregada");
                    c.put("cantidad_entregada", cant);
                    totalEntregado += cant;
                    clientes.add(c);
                }
            }
        }
        resultado.put("clientes_afectados", clientes);
        resultado.put("total_unidades_distribuidas", totalEntregado);
        return resultado;
    }

    // 3. DATOS PARA REPORTE DE SEMÁFORO Y VALOR EN RIESGO
    public List<Map<String, Object>> obtenerReporteSemaforo(String filtroSemaforo) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id_lote, codigo_lote, sku, nombre_producto, nombre_proveedor, " +
                                              "fecha_caducidad, dias_restantes, cantidad_disponible, costo_unitario, " +
                                              "valor_existencia, semaforo, estado FROM v_lote_detalle WHERE 1=1 ");

        if (filtroSemaforo != null && !filtroSemaforo.equalsIgnoreCase("TODOS") && !filtroSemaforo.trim().isEmpty()) {
            sql.append("AND semaforo = '").append(filtroSemaforo.trim().toUpperCase()).append("' ");
        }
        sql.append("ORDER BY fecha_caducidad ASC, id_lote ASC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_lote", rs.getInt("id_lote"));
                fila.put("codigo_lote", rs.getString("codigo_lote"));
                fila.put("sku", rs.getString("sku"));
                fila.put("nombre_producto", rs.getString("nombre_producto"));
                fila.put("nombre_proveedor", rs.getString("nombre_proveedor"));
                Date fc = rs.getDate("fecha_caducidad");
                fila.put("fecha_caducidad", fc != null ? fc.toLocalDate().format(FECHA_FMT) : "");
                fila.put("dias_restantes", rs.getInt("dias_restantes"));
                fila.put("cantidad_disponible", rs.getInt("cantidad_disponible"));
                fila.put("costo_unitario", rs.getBigDecimal("costo_unitario"));
                fila.put("valor_existencia", rs.getBigDecimal("valor_existencia"));
                fila.put("semaforo", rs.getString("semaforo"));
                fila.put("estado", rs.getString("estado"));
                lista.add(fila);
            }
        }
        return lista;
    }

    // 4. DATOS PARA REPORTE FINANCIERO DE VENTAS Y MARGEN BRUTO
    public List<Map<String, Object>> obtenerReporteFinanciero(LocalDate inicio, LocalDate fin) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id_venta, folio, fecha, cliente, total, costo_total, margen_bruto, estado " +
                                              "FROM v_venta_resumen WHERE estado = 'REGISTRADA' ");

        if (inicio != null) {
            sql.append("AND DATE(fecha) >= '").append(inicio).append("' ");
        }
        if (fin != null) {
            sql.append("AND DATE(fecha) <= '").append(fin).append("' ");
        }
        sql.append("ORDER BY fecha ASC, id_venta ASC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_venta", rs.getInt("id_venta"));
                fila.put("folio", rs.getString("folio"));
                Timestamp ts = rs.getTimestamp("fecha");
                fila.put("fecha", ts != null ? ts.toLocalDateTime().format(FECHA_HORA_FMT) : "");
                fila.put("cliente", rs.getString("cliente"));
                fila.put("total", rs.getBigDecimal("total"));
                fila.put("costo_total", rs.getBigDecimal("costo_total"));
                fila.put("margen_bruto", rs.getBigDecimal("margen_bruto"));
                fila.put("estado", rs.getString("estado"));
                lista.add(fila);
            }
        }
        return lista;
    }
}
