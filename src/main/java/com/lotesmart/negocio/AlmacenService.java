package com.lotesmart.negocio;

import com.lotesmart.datos.Conexion;
import com.lotesmart.datos.ErroresBD;
import com.lotesmart.datos.LoteDAO;
import com.lotesmart.modelo.Lote;
import com.lotesmart.modelo.Movimiento;
import com.lotesmart.modelo.SesionActual;
import com.lotesmart.modelo.TipoMovimiento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class AlmacenService {

    private final LoteDAO loteDAO;

    public AlmacenService() {
        this.loteDAO = new LoteDAO();
    }

    public AlmacenService(LoteDAO loteDAO) {
        this.loteDAO = loteDAO;
    }

    public int registrarLote(Lote lote) throws ReglaNegocioException {
        if (lote == null) {
            throw new ReglaNegocioException("Los datos del lote no pueden ser nulos.");
        }
        if (lote.getCodigoLote() == null || lote.getCodigoLote().trim().isEmpty()) {
            throw new ReglaNegocioException("El código de lote es obligatorio.");
        }
        if (lote.getFechaCaducidad() == null) {
            throw new ReglaNegocioException("La fecha de caducidad es obligatoria.");
        }
        if (!lote.getFechaCaducidad().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de caducidad debe ser estrictamente posterior a la fecha actual.");
        }
        if (lote.getCantidadInicial() <= 0) {
            throw new ReglaNegocioException("La cantidad inicial del lote debe ser mayor a 0.");
        }
        if (lote.getCostoUnitario() == null || lote.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El costo unitario debe ser mayor a 0.");
        }

        try {
            return loteDAO.insertar(lote);
        } catch (SQLException e) {
            throw new ReglaNegocioException("No se pudo registrar el lote: " + ErroresBD.traducir(e), e);
        }
    }

    public void actualizarLoteSinMovimientos(int idLote, LocalDate nuevaFecha, BigDecimal nuevoCosto, int nuevaCantidad) throws ReglaNegocioException {
        try {
            if (loteDAO.tieneMovimientos(idLote)) {
                throw new ReglaNegocioException("El lote ya cuenta con movimientos registrados. No se permite la edición directa.");
            }
            if (!nuevaFecha.isAfter(LocalDate.now())) {
                throw new ReglaNegocioException("La nueva fecha de caducidad debe ser posterior a la fecha actual.");
            }
            if (nuevoCosto == null || nuevoCosto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaNegocioException("El costo unitario debe ser mayor a 0.");
            }
            if (nuevaCantidad <= 0) {
                throw new ReglaNegocioException("La cantidad debe ser mayor a 0.");
            }

            boolean ok = loteDAO.actualizarDatosIniciales(idLote, nuevaFecha, nuevoCosto, nuevaCantidad);
            if (!ok) {
                throw new ReglaNegocioException("No se encontró el lote para actualizar.");
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al actualizar lote: " + ErroresBD.traducir(e), e);
        }
    }

    public void bloquearLote(int idLote, String motivo) throws ReglaNegocioException {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ReglaNegocioException("Debe ingresar un motivo para el bloqueo sanitario.");
        }
        try {
            boolean ok = loteDAO.actualizarBloqueo(idLote, true, motivo.trim());
            if (!ok) {
                throw new ReglaNegocioException("No se pudo aplicar el bloqueo sanitario.");
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al bloquear lote: " + ErroresBD.traducir(e), e);
        }
    }

    public void desbloquearLote(int idLote) throws ReglaNegocioException {
        try {
            boolean ok = loteDAO.actualizarBloqueo(idLote, false, null);
            if (!ok) {
                throw new ReglaNegocioException("No se pudo desbloquear el lote.");
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al desbloquear lote: " + ErroresBD.traducir(e), e);
        }
    }

    public void registrarMerma(int idLote, int cantidad, String motivo) throws ReglaNegocioException {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad de merma debe ser mayor a 0.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ReglaNegocioException("El motivo de la merma es obligatorio.");
        }

        Movimiento mov = new Movimiento();
        mov.setIdLote(idLote);
        mov.setTipo(TipoMovimiento.MERMA);
        mov.setCantidad(cantidad);
        mov.setMotivo(motivo.trim());
        mov.setUsuario(SesionActual.getInstance().getUsername());

        try (Connection conn = Conexion.getConexion()) {
            loteDAO.registrarMovimiento(conn, mov);
        } catch (SQLException e) {
            throw new ReglaNegocioException("No se pudo registrar la merma: " + ErroresBD.traducir(e), e);
        }
    }

    public void registrarAjuste(int idLote, int cantidad, boolean positivo, String motivo) throws ReglaNegocioException {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad del ajuste debe ser mayor a 0.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ReglaNegocioException("El motivo del ajuste de inventario es obligatorio.");
        }

        Movimiento mov = new Movimiento();
        mov.setIdLote(idLote);
        mov.setTipo(positivo ? TipoMovimiento.AJUSTE_POSITIVO : TipoMovimiento.AJUSTE_NEGATIVO);
        mov.setCantidad(cantidad);
        mov.setMotivo(motivo.trim());
        mov.setUsuario(SesionActual.getInstance().getUsername());

        try (Connection conn = Conexion.getConexion()) {
            loteDAO.registrarMovimiento(conn, mov);
        } catch (SQLException e) {
            throw new ReglaNegocioException("No se pudo registrar el ajuste: " + ErroresBD.traducir(e), e);
        }
    }

    public void eliminarLote(int idLote) throws ReglaNegocioException {
        try {
            if (loteDAO.tieneMovimientos(idLote)) {
                throw new ReglaNegocioException("El lote tiene movimientos registrados y no puede ser eliminado por razones de auditoría.");
            }
            boolean ok = loteDAO.eliminar(idLote);
            if (!ok) {
                throw new ReglaNegocioException("No se pudo eliminar el lote.");
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error al eliminar lote: " + ErroresBD.traducir(e), e);
        }
    }
}
