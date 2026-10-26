package com.lotesmart.datos;

import com.lotesmart.modelo.Asignacion;
import com.lotesmart.modelo.EstadoVenta;
import com.lotesmart.modelo.FiltroReporte;
import com.lotesmart.modelo.ResumenTablero;
import com.lotesmart.modelo.Venta;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConsultaDAO {

    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void ejecutarSpMarcarCaducados() throws SQLException {
        String sql = "CALL sp_marcar_caducados()";
        try (Connection conn = Conexion.getConexion();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.execute();
        }
    }

    public ResumenTablero obtenerResumenTablero() throws SQLException {
        ResumenTablero r = new ResumenTablero();

        // 1. Dinero en Riesgo y Valor No Vendible desde v_lote_detalle
        String sqlLotes = "SELECT " +
                          "COALESCE(SUM(CASE WHEN semaforo IN ('ROJO', 'AMARILLO') THEN valor_existencia ELSE 0 END), 0) AS valor_riesgo, " +
                          "COALESCE(SUM(CASE WHEN semaforo IN ('CADUCADO', 'BLOQUEADO') THEN valor_existencia ELSE 0 END), 0) AS valor_no_vendible, " +
                          "COUNT(CASE WHEN semaforo = 'CADUCADO' THEN 1 END) AS lotes_caducados, " +
                          "COUNT(CASE WHEN estado = 'VIGENTE' THEN 1 END) AS total_vigentes " +
                          "FROM v_lote_detalle";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlLotes);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                r.setValorEnRiesgo(rs.getBigDecimal("valor_riesgo"));
                r.setValorNoVendible(rs.getBigDecimal("valor_no_vendible"));
                r.setLotesCaducados(rs.getInt("lotes_caducados"));
                r.setTotalLotesVigentes(rs.getInt("total_vigentes"));
            }
        }

        // 2. Alertas de Reorden desde v_stock_producto
        String sqlReorden = "SELECT COUNT(*) FROM v_stock_producto WHERE alerta_reorden = TRUE AND activo = TRUE";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlReorden);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                r.setAlertasReorden(rs.getInt(1));
            }
        }

        // 3. Total ventas hoy
        String sqlVentas = "SELECT COALESCE(SUM(total), 0) FROM venta WHERE DATE(fecha) = CURRENT_DATE AND estado = 'REGISTRADA'";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlVentas);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                r.setTotalVentasHoy(rs.getBigDecimal(1));
            }
        }

        return r;
    }

    public ResultadoTabla obtenerStockProductos() throws SQLException {
        String[] columnas = {"ID", "SKU", "Producto", "Unidad", "Precio Venta", "Stock Vigente", "Stock No Vendible", "Stock Total", "Mínimo", "Reorden"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        String sql = "SELECT id_producto, sku, nombre, unidad, precio_venta, stock_vigente, stock_no_vendible, stock_total, stock_minimo, alerta_reorden " +
                     "FROM v_stock_producto WHERE activo = TRUE ORDER BY nombre ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean reorden = rs.getBoolean("alerta_reorden");
                tabla.agregarFila(new Object[]{
                    rs.getInt("id_producto"),
                    rs.getString("sku"),
                    rs.getString("nombre"),
                    rs.getString("unidad"),
                    String.format("$%.2f", rs.getBigDecimal("precio_venta")),
                    rs.getInt("stock_vigente"),
                    rs.getInt("stock_no_vendible"),
                    rs.getInt("stock_total"),
                    rs.getInt("stock_minimo"),
                    reorden ? "ALERTA (Bajo Stock)" : "Normal"
                });
            }
        }
        return tabla;
    }

    public ResultadoTabla obtenerLotesDetalle(FiltroReporte f) throws SQLException {
        String[] columnas = {"ID", "Código Lote", "Producto", "SKU", "Proveedor", "Caducidad", "Días Rest.", "Disponible", "Inicial", "Costo U.", "Valor Total", "Semáforo", "Estado", "Motivo Bloqueo"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        StringBuilder sql = new StringBuilder("SELECT id_lote, codigo_lote, nombre_producto, sku, nombre_proveedor, fecha_caducidad, dias_restantes, ");
        sql.append("cantidad_disponible, cantidad_inicial, costo_unitario, valor_existencia, semaforo, estado, motivo_bloqueo ");
        sql.append("FROM v_lote_detalle WHERE 1=1 ");

        if (f != null) {
            if (f.getIdProducto() != null && f.getIdProducto() > 0) {
                sql.append("AND id_producto = ").append(f.getIdProducto()).append(" ");
            }
            if (f.getSemaforo() != null && !f.getSemaforo().equalsIgnoreCase("TODOS")) {
                sql.append("AND semaforo = '").append(f.getSemaforo()).append("' ");
            }
            if (f.getCodigoLote() != null && !f.getCodigoLote().trim().isEmpty()) {
                sql.append("AND LOWER(codigo_lote) LIKE LOWER('%").append(f.getCodigoLote().trim()).append("%') ");
            }
        }
        sql.append("ORDER BY fecha_caducidad ASC, id_lote ASC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date cad = rs.getDate("fecha_caducidad");
                tabla.agregarFila(new Object[]{
                    rs.getInt("id_lote"),
                    rs.getString("codigo_lote"),
                    rs.getString("nombre_producto"),
                    rs.getString("sku"),
                    rs.getString("nombre_proveedor"),
                    cad != null ? cad.toLocalDate().format(FECHA_FMT) : "",
                    rs.getInt("dias_restantes"),
                    rs.getInt("cantidad_disponible"),
                    rs.getInt("cantidad_inicial"),
                    String.format("$%.2f", rs.getBigDecimal("costo_unitario")),
                    String.format("$%.2f", rs.getBigDecimal("valor_existencia")),
                    rs.getString("semaforo"),
                    rs.getString("estado"),
                    rs.getString("motivo_bloqueo") != null ? rs.getString("motivo_bloqueo") : ""
                });
            }
        }
        return tabla;
    }

    public ResultadoTabla obtenerVentas(FiltroReporte f) throws SQLException {
        String[] columnas = {"ID", "Folio", "Fecha / Hora", "Cliente", "Total", "Costo Total", "Margen Bruto", "Estado", "Motivo Cancelación"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        StringBuilder sql = new StringBuilder("SELECT id_venta, folio, fecha, cliente, total, costo_total, margen_bruto, estado, motivo_cancelacion ");
        sql.append("FROM v_venta_resumen WHERE 1=1 ");

        if (f != null) {
            if (f.getFechaInicio() != null) {
                sql.append("AND DATE(fecha) >= '").append(f.getFechaInicio()).append("' ");
            }
            if (f.getFechaFin() != null) {
                sql.append("AND DATE(fecha) <= '").append(f.getFechaFin()).append("' ");
            }
            if (f.getIdCliente() != null && f.getIdCliente() > 0) {
                sql.append("AND id_cliente = ").append(f.getIdCliente()).append(" ");
            }
        }
        sql.append("ORDER BY id_venta DESC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha");
                tabla.agregarFila(new Object[]{
                    rs.getInt("id_venta"),
                    rs.getString("folio"),
                    ts != null ? ts.toLocalDateTime().format(FECHA_HORA_FMT) : "",
                    rs.getString("cliente"),
                    String.format("$%.2f", rs.getBigDecimal("total")),
                    String.format("$%.2f", rs.getBigDecimal("costo_total")),
                    String.format("$%.2f", rs.getBigDecimal("margen_bruto")),
                    rs.getString("estado"),
                    rs.getString("motivo_cancelacion") != null ? rs.getString("motivo_cancelacion") : ""
                });
            }
        }
        return tabla;
    }

    public Venta obtenerVentaPorId(int idVenta) throws SQLException {
        String sql = "SELECT id_venta, folio, fecha, id_cliente, cliente, id_usuario, username, total, estado, " +
                     "motivo_cancelacion, fecha_cancelacion, costo_total, margen_bruto " +
                     "FROM v_venta_resumen WHERE id_venta = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venta v = new Venta();
                    v.setIdVenta(rs.getInt("id_venta"));
                    v.setFolio(rs.getString("folio"));
                    Timestamp ts = rs.getTimestamp("fecha");
                    if (ts != null) v.setFecha(ts.toLocalDateTime());
                    v.setIdCliente(rs.getInt("id_cliente"));
                    v.setCliente(rs.getString("cliente"));
                    int idUsr = rs.getInt("id_usuario");
                    if (!rs.wasNull()) v.setIdUsuario(idUsr);
                    v.setUsername(rs.getString("username"));
                    v.setTotal(rs.getBigDecimal("total"));
                    v.setEstado(EstadoVenta.fromString(rs.getString("estado")));
                    v.setMotivoCancelacion(rs.getString("motivo_cancelacion"));
                    Timestamp tsc = rs.getTimestamp("fecha_cancelacion");
                    if (tsc != null) v.setFechaCancelacion(tsc.toLocalDateTime());
                    v.setCostoTotal(rs.getBigDecimal("costo_total"));
                    v.setMargenBruto(rs.getBigDecimal("margen_bruto"));
                    return v;
                }
            }
        }
        return null;
    }

    public List<Asignacion> obtenerMovimientosVenta(int idVenta) throws SQLException {
        List<Asignacion> lista = new ArrayList<>();
        String sql = "SELECT m.id_lote, l.codigo_lote, l.fecha_caducidad, m.cantidad, l.costo_unitario, m.precio_unitario " +
                     "FROM movimiento m " +
                     "JOIN lote l ON m.id_lote = l.id_lote " +
                     "WHERE m.id_venta = ? AND m.tipo = 'VENTA' " +
                     "ORDER BY m.id_movimiento ASC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asignacion a = new Asignacion();
                    a.setIdLote(rs.getInt("id_lote"));
                    a.setCodigoLote(rs.getString("codigo_lote"));
                    Date d = rs.getDate("fecha_caducidad");
                    if (d != null) a.setFechaCaducidad(d.toLocalDate());
                    a.setCantidad(rs.getInt("cantidad"));
                    a.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
                    a.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    lista.add(a);
                }
            }
        }
        return lista;
    }

    public ResultadoTabla obtenerMovimientos(FiltroReporte f) throws SQLException {
        String[] columnas = {"ID", "Fecha / Hora", "Tipo", "Lote", "Producto", "SKU", "Cantidad", "Precio U.", "Folio Venta", "Motivo", "Usuario"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        StringBuilder sql = new StringBuilder("SELECT m.id_movimiento, m.fecha, m.tipo, l.codigo_lote, p.nombre AS producto, p.sku, ");
        sql.append("m.cantidad, m.precio_unitario, ('V-' || LPAD(m.id_venta::text, 6, '0')) AS folio_venta, m.motivo, m.usuario ");
        sql.append("FROM movimiento m ");
        sql.append("JOIN lote l ON m.id_lote = l.id_lote ");
        sql.append("JOIN producto p ON l.id_producto = p.id_producto WHERE 1=1 ");

        if (f != null) {
            if (f.getFechaInicio() != null) {
                sql.append("AND DATE(m.fecha) >= '").append(f.getFechaInicio()).append("' ");
            }
            if (f.getFechaFin() != null) {
                sql.append("AND DATE(m.fecha) <= '").append(f.getFechaFin()).append("' ");
            }
            if (f.getTipoMovimiento() != null) {
                sql.append("AND m.tipo = '").append(f.getTipoMovimiento().name()).append("' ");
            }
            if (f.getCodigoLote() != null && !f.getCodigoLote().trim().isEmpty()) {
                sql.append("AND LOWER(l.codigo_lote) LIKE LOWER('%").append(f.getCodigoLote().trim()).append("%') ");
            }
        }
        sql.append("ORDER BY m.id_movimiento DESC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha");
                BigDecimal pu = rs.getBigDecimal("precio_unitario");
                tabla.agregarFila(new Object[]{
                    rs.getInt("id_movimiento"),
                    ts != null ? ts.toLocalDateTime().format(FECHA_HORA_FMT) : "",
                    rs.getString("tipo"),
                    rs.getString("codigo_lote"),
                    rs.getString("producto"),
                    rs.getString("sku"),
                    rs.getInt("cantidad"),
                    pu != null ? String.format("$%.2f", pu) : "-",
                    rs.getString("folio_venta") != null && !rs.getString("folio_venta").contains("NULL") ? rs.getString("folio_venta") : "-",
                    rs.getString("motivo") != null ? rs.getString("motivo") : "",
                    rs.getString("usuario")
                });
            }
        }
        return tabla;
    }

    public ResultadoTabla obtenerAuditoria(FiltroReporte f) throws SQLException {
        String[] columnas = {"ID", "Fecha / Hora", "Tabla", "ID Registro", "Acción", "Detalle", "Usuario"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        StringBuilder sql = new StringBuilder("SELECT id_auditoria, fecha, tabla, id_registro, accion, detalle, usuario ");
        sql.append("FROM auditoria WHERE 1=1 ");

        if (f != null) {
            if (f.getFechaInicio() != null) {
                sql.append("AND DATE(fecha) >= '").append(f.getFechaInicio()).append("' ");
            }
            if (f.getFechaFin() != null) {
                sql.append("AND DATE(fecha) <= '").append(f.getFechaFin()).append("' ");
            }
            if (f.getTabla() != null && !f.getTabla().equalsIgnoreCase("TODAS") && !f.getTabla().trim().isEmpty()) {
                sql.append("AND LOWER(tabla) = LOWER('").append(f.getTabla().trim()).append("') ");
            }
        }
        sql.append("ORDER BY id_auditoria DESC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha");
                tabla.agregarFila(new Object[]{
                    rs.getInt("id_auditoria"),
                    ts != null ? ts.toLocalDateTime().format(FECHA_HORA_FMT) : "",
                    rs.getString("tabla"),
                    rs.getInt("id_registro"),
                    rs.getString("accion"),
                    rs.getString("detalle"),
                    rs.getString("usuario")
                });
            }
        }
        return tabla;
    }

    public ResultadoTabla obtenerTrazabilidad(String codigoLote) throws SQLException {
        String[] columnas = {"Folio Venta", "Fecha Venta", "Lote", "Caducidad", "SKU", "Producto", "Cliente", "Teléfono", "Correo", "Cantidad Entregada"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        String sql = "SELECT folio_venta, fecha_venta, codigo_lote, fecha_caducidad, sku, nombre_producto, " +
                     "nombre_cliente, telefono_cliente, correo_cliente, cantidad_entregada " +
                     "FROM v_trazabilidad_lote WHERE LOWER(codigo_lote) = LOWER(?) ORDER BY fecha_venta DESC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoLote.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp fv = rs.getTimestamp("fecha_venta");
                    Date fc = rs.getDate("fecha_caducidad");
                    tabla.agregarFila(new Object[]{
                        rs.getString("folio_venta"),
                        fv != null ? fv.toLocalDateTime().format(FECHA_HORA_FMT) : "",
                        rs.getString("codigo_lote"),
                        fc != null ? fc.toLocalDate().format(FECHA_FMT) : "",
                        rs.getString("sku"),
                        rs.getString("nombre_producto"),
                        rs.getString("nombre_cliente"),
                        rs.getString("telefono_cliente") != null ? rs.getString("telefono_cliente") : "-",
                        rs.getString("correo_cliente") != null ? rs.getString("correo_cliente") : "-",
                        rs.getInt("cantidad_entregada")
                    });
                }
            }
        }
        return tabla;
    }

    public ResultadoTabla obtenerComprasCliente(int idCliente) throws SQLException {
        String[] columnas = {"ID", "Folio", "Fecha / Hora", "Total", "Estado", "Lotes Recibidos (Código / Caducidad / Cantidad)"};
        ResultadoTabla tabla = new ResultadoTabla(columnas);

        String sql = "SELECT v.id_venta, ('V-' || LPAD(v.id_venta::text, 6, '0')) as folio, v.fecha, v.total, v.estado, " +
                     "COALESCE(string_agg(l.codigo_lote || ' [Cad: ' || l.fecha_caducidad || ' x ' || m.cantidad || ' uds]', ' | '), 'Sin desglose') as lotes_recibidos " +
                     "FROM venta v " +
                     "LEFT JOIN movimiento m ON v.id_venta = m.id_venta AND m.tipo = 'VENTA' " +
                     "LEFT JOIN lote l ON m.id_lote = l.id_lote " +
                     "WHERE v.id_cliente = ? " +
                     "GROUP BY v.id_venta, v.fecha, v.total, v.estado " +
                     "ORDER BY v.id_venta DESC";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha");
                    tabla.agregarFila(new Object[]{
                        rs.getInt("id_venta"),
                        rs.getString("folio"),
                        ts != null ? ts.toLocalDateTime().format(FECHA_HORA_FMT) : "",
                        String.format("$%.2f", rs.getBigDecimal("total")),
                        rs.getString("estado"),
                        rs.getString("lotes_recibidos")
                    });
                }
            }
        }
        return tabla;
    }
}
