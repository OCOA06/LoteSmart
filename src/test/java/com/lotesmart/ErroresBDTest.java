package com.lotesmart;

import com.lotesmart.datos.ErroresBD;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pruebas Unitarias de Traducción de Errores SQL (PostgreSQL)")
class ErroresBDTest {

    @Test
    @DisplayName("Debe traducir error de unicidad (23505)")
    void testTraduccionUnique() {
        SQLException sqlEx = new SQLException("duplicate key value violates unique constraint \"producto_sku_key\"", "23505");
        String mensaje = ErroresBD.traducir(sqlEx);
        assertTrue(mensaje.contains("SKU") || mensaje.contains("unicidad"));
    }

    @Test
    @DisplayName("Debe traducir error de regla de negocio lanzada por trigger PL/pgSQL (P0001)")
    void testTraduccionTriggerP0001() {
        SQLException sqlEx = new SQLException("ERROR: La fecha de caducidad debe ser posterior a la fecha actual", "P0001");
        String mensaje = ErroresBD.traducir(sqlEx);
        assertTrue(mensaje.contains("posterior a la fecha actual"));
    }

    @Test
    @DisplayName("Debe traducir error de llave foránea restrictiva (23503)")
    void testTraduccionFkRestrict() {
        SQLException sqlEx = new SQLException("violates foreign key constraint", "23503");
        String mensaje = ErroresBD.traducir(sqlEx);
        assertTrue(mensaje.contains("referenciado") || mensaje.contains("vinculado"));
    }

    @Test
    @DisplayName("Debe traducir error de validación check (23514)")
    void testTraduccionCheck() {
        SQLException sqlEx = new SQLException("violates check constraint \"cliente_rfc_check\"", "23514");
        String mensaje = ErroresBD.traducir(sqlEx);
        assertTrue(mensaje.contains("RFC"));
    }
}
