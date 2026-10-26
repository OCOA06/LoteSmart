package com.lotesmart.negocio;

import com.lotesmart.datos.Conexion;
import com.lotesmart.datos.ErroresBD;
import com.lotesmart.datos.LoteDAO;
import com.lotesmart.modelo.Asignacion;
import com.lotesmart.modelo.Cliente;
import com.lotesmart.modelo.LineaVenta;
import com.lotesmart.modelo.LoteDisponible;
import com.lotesmart.modelo.Movimiento;
import com.lotesmart.modelo.Producto;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.TipoMovimiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VentaService {

    private final FefoAllocator fefoAllocator;
    private final LoteDAO loteDAO;

    public VentaService() {
        this.fefoAllocator = new FefoAllocator();
        this.loteDAO = new LoteDAO();
    }

    public VentaService(FefoAllocator fefoAllocator, LoteDAO loteDAO) {
        this.fefoAllocator = fefoAllocator;
        this.loteDAO = loteDAO;
    }

    /**
     * Simula la asignación FEFO en memoria para todos los productos del carrito
     * sin alterar el inventario en la base de datos.
     */
    public void simularFefo(List<LineaVenta> carrito, LocalDate hoy) throws ReglaNegocioException, SQLException {
        if (carrito == null || carrito.isEmpty()) {
            throw new ReglaNegocioException("El carrito de compra se encuentra vacío.");
        }
        if (hoy == null) {
            hoy = LocalDate.now();
        }

        for (LineaVenta linea : carrito) {
            Producto prod = linea.getProducto();
            List<LoteDisponible> lotes = loteDAO.listarDisponiblesPorProducto(prod.getIdProducto());
            List<Asignacion> asignaciones = fefoAllocator.asignarLotes(prod, lotes, linea.getCantidad(), hoy);
            linea.setAsignaciones(asignaciones);
        }
    }

    /**
     * Registra la venta dentro de una transacción JDBC estricta con SELECT FOR UPDATE.
     */
    public int registrarVenta(int idCliente, String nombreCliente, Integer idUsuario, List<LineaVenta> carrito)
            throws ReglaNegocioException, SQLException {

        if (carrito == null || carrito.isEmpty()) {
            throw new ReglaNegocioException("El carrito no contiene productos para procesar la venta.");
        }
        if (nombreCliente == null || nombreCliente.trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del cliente es obligatorio.");
        }

        BigDecimal totalVenta = BigDecimal.ZERO;
        for (LineaVenta linea : carrito) {
            if (linea.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad para '" + linea.getProducto().getNombre() + "' debe ser mayor a 0.");
            }
            totalVenta = totalVenta.add(linea.getSubtotal());
        }

        LocalDate hoy = LocalDate.now();
        String usuarioActual = SesionActual.getInstance().getUsername();

        Connection conn = null;
        try {
            conn = Conexion.getConexion();
            conn.setAutoCommit(false);
            Conexion.inyectarUsuario(conn); // Inyectar usuario en la transacción (is_local = true) // Iniciar transacción explícita
            Conexion.inyectarUsuario(conn); // Inyectar usuario en la transacción (is_local = true)

            // 1. Insertar cabecera de venta
            int idVentaGenerado = 0;
            String sqlVenta = "INSERT INTO venta (id_cliente, cliente, id_usuario, total, estado) " +
                              "VALUES (?, ?, ?, ?, 'REGISTRADA') RETURNING id_venta";
            try (PreparedStatement psVenta = conn.prepareStatement(sqlVenta)) {
                psVenta.setInt(1, idCliente);
                psVenta.setString(2, nombreCliente.trim());
                if (idUsuario != null && idUsuario > 0) {
                    psVenta.setInt(3, idUsuario);
                } else {
                    psVenta.setNull(3, Types.INTEGER);
                }
                psVenta.setBigDecimal(4, totalVenta);
                try (ResultSet rs = psVenta.executeQuery()) {
                    if (rs.next()) {
                        idVentaGenerado = rs.getInt(1);
                    }
                }
            }

            if (idVentaGenerado <= 0) {
                throw new SQLException("No se pudo generar el folio de venta en la base de datos.");
            }

            // 2. Procesar cada producto del carrito bajo bloqueo FOR UPDATE
            for (LineaVenta linea : carrito) {
                Producto prod = linea.getProducto();
                List<LoteDisponible> lotesBloqueados = loteDAO.listarDisponiblesPorProductoForUpdate(conn, prod.getIdProducto());

                // Calcular asignación FEFO definitiva sobre los lotes bloqueados
                List<Asignacion> asignaciones = fefoAllocator.asignarLotes(prod, lotesBloqueados, linea.getCantidad(), hoy);
                linea.setAsignaciones(asignaciones);

                // Insertar los movimientos de venta
                for (Asignacion a : asignaciones) {
                    Movimiento mov = new Movimiento();
                    mov.setIdLote(a.getIdLote());
                    mov.setIdVenta(idVentaGenerado);
                    mov.setTipo(TipoMovimiento.VENTA);
                    mov.setCantidad(a.getCantidad());
                    mov.setPrecioUnitario(a.getPrecioUnitario());
                    mov.setMotivo("Despacho FEFO Venta Folio " + idVentaGenerado);
                    mov.setUsuario(usuarioActual);

                    loteDAO.registrarMovimiento(conn, mov);
                }
            }

            conn.commit(); // Confirmar transacción exitosa
            return idVentaGenerado;

        } catch (SQLException | ReglaNegocioException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException exRollback) {
                    // ignorar
                }
            }
            if (e instanceof ReglaNegocioException) {
                throw (ReglaNegocioException) e;
            }
            throw new ReglaNegocioException("Error al registrar la venta: " + ErroresBD.traducir(e), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // ignorar
                }
            }
        }
    }

    /**
     * Cancela formalmente una venta registrada e inserta movimientos de DEVOLUCION
     * regresando cada unidad a su lote original exacto.
     */
    public void cancelarVenta(int idVenta, String motivo) throws ReglaNegocioException, SQLException {
        if (idVenta <= 0) {
            throw new ReglaNegocioException("Folio de venta inválido.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ReglaNegocioException("Es obligatorio especificar el motivo de cancelación.");
        }

        String usuarioActual = SesionActual.getInstance().getUsername();
        Connection conn = null;
        try {
            conn = Conexion.getConexion();
            conn.setAutoCommit(false);
            Conexion.inyectarUsuario(conn); // Inyectar usuario en la transacción (is_local = true)

            // 1. Bloquear y verificar estado de la venta
            String sqlEstado = "SELECT estado FROM venta WHERE id_venta = ? FOR UPDATE";
            String estadoActual = null;
            try (PreparedStatement psEstado = conn.prepareStatement(sqlEstado)) {
                psEstado.setInt(1, idVenta);
                try (ResultSet rs = psEstado.executeQuery()) {
                    if (rs.next()) {
                        estadoActual = rs.getString("estado");
                    }
                }
            }

            if (estadoActual == null) {
                throw new ReglaNegocioException("La venta con ID " + idVenta + " no existe.");
            }
            if ("CANCELADA".equalsIgnoreCase(estadoActual)) {
                throw new ReglaNegocioException("La venta ya se encuentra CANCELADA.");
            }

            // 2. Obtener todos los movimientos de salida tipo VENTA asociados a esta venta
            String sqlMovs = "SELECT id_lote, cantidad, precio_unitario FROM movimiento " +
                             "WHERE id_venta = ? AND tipo = 'VENTA'";
            List<Movimiento> devoluciones = new ArrayList<>();
            try (PreparedStatement psMovs = conn.prepareStatement(sqlMovs)) {
                psMovs.setInt(1, idVenta);
                try (ResultSet rs = psMovs.executeQuery()) {
                    while (rs.next()) {
                        Movimiento dev = new Movimiento();
                        dev.setIdLote(rs.getInt("id_lote"));
                        dev.setIdVenta(idVenta);
                        dev.setTipo(TipoMovimiento.DEVOLUCION);
                        dev.setCantidad(rs.getInt("cantidad"));
                        dev.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                        dev.setMotivo("Cancelación de venta: " + motivo.trim());
                        dev.setUsuario(usuarioActual);
                        devoluciones.add(dev);
                    }
                }
            }

            // 3. Insertar cada movimiento de DEVOLUCION (los triggers de BD repondrán las existencias en cada lote)
            for (Movimiento dev : devoluciones) {
                loteDAO.registrarMovimiento(conn, dev);
            }

            // 4. Actualizar estado de la venta a CANCELADA
            String sqlUpdateVenta = "UPDATE venta SET estado = 'CANCELADA', motivo_cancelacion = ?, fecha_cancelacion = CURRENT_TIMESTAMP " +
                                    "WHERE id_venta = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateVenta)) {
                psUpdate.setString(1, motivo.trim());
                psUpdate.setInt(2, idVenta);
                psUpdate.executeUpdate();
            }

            conn.commit();

        } catch (SQLException | ReglaNegocioException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException exRollback) {
                    // ignorar
                }
            }
            if (e instanceof ReglaNegocioException) {
                throw (ReglaNegocioException) e;
            }
            throw new ReglaNegocioException("Error al cancelar la venta: " + ErroresBD.traducir(e), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // ignorar
                }
            }
        }
    }
}
