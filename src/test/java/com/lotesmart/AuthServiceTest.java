package com.lotesmart;

import com.lotesmart.negocio.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Pruebas Unitarias de Seguridad y Hash SHA-256")
class AuthServiceTest {

    @Test
    @DisplayName("Debe generar hash SHA-256 consistente y coincidente con el seed data de PostgreSQL")
    void testHashSha256() {
        // admin123
        String hashAdmin = AuthService.hashSha256("admin123");
        assertEquals("240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9", hashAdmin);

        // operador123
        String hashOperador = AuthService.hashSha256("operador123");
        assertEquals("1725165c9a0b3698a3d01016e0d8205155820b8d7f21835ca64c0f81c728d880", hashOperador);

        // cliente123
        String hashCliente = AuthService.hashSha256("cliente123");
        assertEquals("09a31a7001e261ab1e056182a71d3cf57f582ca9a29cff5eb83be0f0549730a9", hashCliente);
    }

    @Test
    @DisplayName("Hashes de diferentes contraseñas deben ser distintos")
    void testHashesDistintos() {
        String h1 = AuthService.hashSha256("admin123");
        String h2 = AuthService.hashSha256("admin124");
        assertNotEquals(h1, h2);
    }
}
