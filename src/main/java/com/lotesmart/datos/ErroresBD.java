package com.lotesmart.datos;

import java.sql.SQLException;

public class ErroresBD {

    private ErroresBD() {
    }

    public static String traducir(Exception ex) {
        if (ex == null) {
            return "Ha ocurrido un error inesperado.";
        }
        if (ex instanceof SQLException) {
            return traducir((SQLException) ex);
        }
        Throwable cause = ex.getCause();
        if (cause instanceof SQLException) {
            return traducir((SQLException) cause);
        }
        return ex.getMessage() != null ? ex.getMessage() : "Error en el sistema: " + ex.getClass().getSimpleName();
    }

    public static String traducir(SQLException sqlEx) {
        if (sqlEx == null) {
            return "Error desconocido en la base de datos.";
        }

        String sqlState = sqlEx.getSQLState();
        String rawMessage = sqlEx.getMessage();
        if (rawMessage != null) {
            rawMessage = rawMessage.trim();
            // Limpiar prefijo de postgres "ERROR: " si existe
            if (rawMessage.startsWith("ERROR:")) {
                rawMessage = rawMessage.substring(6).trim();
            }
            int whereIdx = rawMessage.indexOf("\n  Where:");
            if (whereIdx > 0) {
                rawMessage = rawMessage.substring(0, whereIdx).trim();
            }
        }

        if (sqlState == null) {
            return rawMessage != null && !rawMessage.isEmpty() ? rawMessage : "Error general de base de datos.";
        }

        switch (sqlState) {
            case "P0001":
                // Excepción personalizada lanzada por RAISE EXCEPTION en triggers PL/pgSQL
                return rawMessage != null && !rawMessage.isEmpty() ? rawMessage : "Violación de regla de negocio en el almacén.";

            case "23505":
                // Violación de restricción UNIQUE
                if (rawMessage != null) {
                    if (rawMessage.contains("usuario_username_key")) {
                        return "El nombre de usuario ya está registrado en el sistema.";
                    } else if (rawMessage.contains("cliente_rfc_key")) {
                        return "El RFC ya se encuentra asignado a otro cliente.";
                    } else if (rawMessage.contains("proveedor_rfc_key")) {
                        return "El RFC ya se encuentra asignado a otro proveedor.";
                    } else if (rawMessage.contains("producto_sku_key")) {
                        return "El SKU ya se encuentra registrado para otro producto.";
                    } else if (rawMessage.contains("uq_lote_producto_codigo")) {
                        return "Ya existe un lote con ese código para el producto seleccionado.";
                    }
                }
                return "Operación rechazada: Ya existe un registro con los mismos datos clave (Violación de unicidad).";

            case "23503":
                // Violación de llave foránea (FK RESTRICT)
                return "Operación rechazada: El registro no puede ser eliminado ni modificado porque está vinculado a otras operaciones activas (ventas, movimientos o catálogo).";

            case "23514":
                // Violación de restricción CHECK
                if (rawMessage != null) {
                    if (rawMessage.contains("cliente_rfc_check") || rawMessage.contains("proveedor_rfc_check")) {
                        return "El RFC debe tener exactamente 12 o 13 caracteres alfanuméricos válidos.";
                    } else if (rawMessage.contains("producto_precio_venta_check")) {
                        return "El precio de venta debe ser un número positivo mayor a cero.";
                    } else if (rawMessage.contains("lote_cantidad_disponible_check")) {
                        return "La cantidad disponible no puede ser negativa ni superar la cantidad inicial del lote.";
                    } else if (rawMessage.contains("chk_movimiento_venta")) {
                        return "Los movimientos de venta y devolución deben incluir folio de venta; las mermas y ajustes deben ser independientes.";
                    }
                }
                return "Operación rechazada: Uno o más valores no cumplen las reglas de integridad de la base de datos.";

            case "23502":
                // Not null violation
                return "Operación rechazada: Faltan campos obligatorios para completar el registro.";

            case "08001":
            case "08006":
                return "No fue posible conectar con el servidor Supabase en la nube. Verifique su conexión a internet.";

            default:
                return rawMessage != null && !rawMessage.isEmpty() ? rawMessage : ("Error de base de datos (Código: " + sqlState + ")");
        }
    }
}
