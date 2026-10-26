package com.lotesmart;

import com.lotesmart.datos.Conexion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Configuración de Conexión a Supabase")
class ConexionConfigTest {

    @Test
    @DisplayName("El driver de PostgreSQL debe cargarse exitosamente")
    void testDriverPostgresCarga() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"),
                "El driver org.postgresql.Driver debe estar presente en el ClassLoader");
    }

    @Test
    @DisplayName("Las constantes por defecto de Conexion deben proteger la contraseña")
    void testConstantesConexionSupabase() {
        assertEquals("jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require", Conexion.DEFAULT_URL);
        assertEquals("postgres.eeiktpvbttbvrvziwwjn", Conexion.DEFAULT_USER);
        assertEquals("", Conexion.DEFAULT_PASSWORD);
    }

    @Test
    @DisplayName("El archivo de plantilla db.properties.example debe existir con parámetros protegidos")
    void testArchivoDbProperties() throws Exception {
        File file = new File("db.properties.example");
        assertTrue(file.exists(), "El archivo db.properties.example debe existir en la raíz del proyecto");

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");

        assertNotNull(url, "db.url no debe ser nulo");
        assertTrue(url.contains("pooler.supabase.com"), "La URL debe apuntar al Session Pooler compatible IPv4");
        assertTrue(url.contains("sslmode=require"), "La URL debe requerir SSL (sslmode=require)");
        assertNotNull(user, "El usuario debe estar definido en la plantilla");
        assertEquals("TU_PASSWORD_AQUI", pass, "La plantilla no debe contener contraseñas reales");
    }

    @Test
    @DisplayName("La clase Conexion debe poseer los métodos de inyección de usuario en sesión y transacción")
    void testMetodosInyeccionUsuario() {
        assertDoesNotThrow(() -> {
            Conexion.class.getMethod("inyectarUsuario", Connection.class);
            Conexion.class.getMethod("inyectarUsuario", Connection.class, String.class);
            Conexion.class.getMethod("inyectarUsuario", Connection.class, String.class, boolean.class);
            Conexion.class.getMethod("probarConexion");
            Conexion.class.getMethod("cerrarPool");
        }, "Conexion debe exponer los métodos requeridos para inyección de usuario y administración del pool");
    }
}
