package com.lotesmart.datos;

import com.lotesmart.modelo.SesionActual;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor de conexiones a la base de datos PostgreSQL en Supabase.
 * Administra el pool de conexiones con HikariCP, asegura el uso de SSL obligatorio
 * y mantiene la inyección de usuario en la sesión para auditoría mediante triggers.
 */
public class Conexion {
    private static final Logger LOGGER = Logger.getLogger(Conexion.class.getName());
    private static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";
    
    // Valores por defecto para conexión mediante Session Pooler hacia Supabase (IPv4 e IPv6)
    public static final String DEFAULT_URL = "jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require";
    public static final String DEFAULT_USER = "postgres.eeiktpvbttbvrvziwwjn";
    public static final String DEFAULT_PASSWORD = "";

    private static HikariDataSource dataSource;

    static {
        // Carga explícita del Driver PostgreSQL en el ClassLoader
        try {
            Class.forName(DRIVER_CLASS_NAME);
            LOGGER.info("Driver JDBC PostgreSQL (" + DRIVER_CLASS_NAME + ") cargado correctamente.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Fallo al cargar el driver PostgreSQL: " + DRIVER_CLASS_NAME, e);
        }
    }

    private Conexion() {
    }

    /**
     * Inicializa el pool de conexiones HikariCP leyendo la configuración desde
     * variables de entorno, db.properties en la raíz o db.properties en el classpath.
     */
    private static synchronized void inicializarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        // Asegurar que el driver esté cargado
        try {
            Class.forName(DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver PostgreSQL no disponible: " + e.getMessage(), e);
            throw new RuntimeException("Driver PostgreSQL no encontrado en el classpath.", e);
        }

        Properties props = new Properties();
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
                LOGGER.info("Configuración cargada desde db.properties en raíz.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo leer db.properties en raíz: " + e.getMessage(), e);
            }
        } else {
            try (InputStream in = Conexion.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    LOGGER.info("Configuración cargada desde classpath db.properties.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo leer db.properties de classpath: " + e.getMessage(), e);
            }
        }

        // 1. Obtener URL (Prioridad: Variable de entorno > db.properties > Default)
        String url = System.getenv("DB_URL");
        if (url == null || url.trim().isEmpty()) {
            url = props.getProperty("db.url", DEFAULT_URL);
        }
        
        // Limpieza estricta de caracteres espurios, saltos de línea, comillas o espacios invisibles
        url = url.trim().replaceAll("[\r\n\t\"']", "");
        if (url.startsWith("URL:")) {
            url = url.substring(4).trim();
        }
        if (url.contains("[") || url.contains("]")) {
            url = url.replaceAll("\\[|\\]|\\(.*\\)", "").trim();
        }

        // Forzar sslmode=require si no está especificado en la URL
        if (!url.contains("sslmode=")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        // 2. Obtener Usuario
        String user = System.getenv("DB_USER");
        if (user == null || user.trim().isEmpty()) {
            user = props.getProperty("db.user", DEFAULT_USER);
        }
        user = user.trim().replaceAll("[\r\n\t\"']", "");

        // 3. Obtener Contraseña
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.trim().isEmpty()) {
            password = props.getProperty("db.password", DEFAULT_PASSWORD);
        }
        password = password.trim().replaceAll("[\r\n]", "");
        if (password.isEmpty()) {
            LOGGER.log(Level.SEVERE, "La contraseña de la base de datos no está configurada.");
            throw new IllegalStateException("Contraseña de base de datos no configurada. " +
                    "Por favor define 'db.password' en el archivo 'db.properties' o en la variable de entorno DB_PASSWORD.");
        }

        // 4. Configurar HikariCP
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DRIVER_CLASS_NAME);
        config.setJdbcUrl(url.trim());
        config.setUsername(user.trim());
        config.setPassword(password.trim());

        // Optimización del pool hacia Supabase
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(15000);
        config.setMaxLifetime(600000);
        config.setPoolName("LoteSmart-SupabasePool");

        // Forzar cifrado SSL obligatorio para PostgreSQL / Supabase
        config.addDataSourceProperty("ssl", "true");
        config.addDataSourceProperty("sslmode", "require");
        config.addDataSourceProperty("ApplicationName", "LoteSmartEnterprise");

        try {
            dataSource = new HikariDataSource(config);
            LOGGER.info("Pool HikariCP inicializado exitosamente hacia: " + url);
        } catch (Exception e) {
            String msg = e.getMessage();
            Throwable cause = e.getCause();
            boolean isDnsError = (e instanceof java.net.UnknownHostException) || 
                                 (cause instanceof java.net.UnknownHostException) ||
                                 (msg != null && (msg.contains("UnknownHostException") || msg.contains("Host desconocido")));
            if (isDnsError) {
                msg = "No se pudo resolver el host de Supabase (" + url + ").\n" +
                      "Supabase utiliza IPv6 para conexiones directas (db.*.supabase.co). " +
                      "Si tu red/router no tiene soporte IPv6 público, debes usar el Connection Pooler " +
                      "de Supabase (modo Session o Transaction) en formato: aws-0-[region].pooler.supabase.com:5432 " +
                      "con usuario postgres.[ref_proyecto]";
            }
            LOGGER.log(Level.SEVERE, "Fallo al inicializar HikariCP: " + msg, e);
            throw new RuntimeException("Error al conectar con el servidor Supabase: " + msg, e);
        }
    }

    /**
     * Obtiene una conexión activa del pool de HikariCP y preconfigura el usuario
     * de la sesión actual en PostgreSQL para alimentar los triggers de auditoría.
     */
    public static Connection getConexion() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            inicializarPool();
        }
        Connection conn = dataSource.getConnection();
        
        // Inyectar el usuario actual en la sesión para que los triggers de auditoría funcionen
        inyectarUsuario(conn, SesionActual.getInstance().getUsername(), false);

        return conn;
    }

    /**
     * Inyecta el usuario actual de SesionActual en la transacción activa (is_local = true).
     * Ejecuta: SELECT set_config('app.usuario', ?, true);
     *
     * @param conn Conexión JDBC activa
     */
    public static void inyectarUsuario(Connection conn) {
        inyectarUsuario(conn, SesionActual.getInstance().getUsername(), true);
    }

    /**
     * Inyecta un usuario específico en la transacción activa (is_local = true).
     * Ejecuta: SELECT set_config('app.usuario', ?, true);
     *
     * @param conn Conexión JDBC activa
     * @param usuario Nombre del usuario
     */
    public static void inyectarUsuario(Connection conn, String usuario) {
        inyectarUsuario(conn, usuario, true);
    }

    /**
     * Inyecta el usuario en la sesión o transacción de PostgreSQL mediante SELECT set_config('app.usuario', ?, ?).
     *
     * @param conn Conexión JDBC activa
     * @param usuario Nombre del usuario a auditar (si es nulo o vacío, usa "SISTEMA")
     * @param isLocal true para ámbito transaccional (SET LOCAL), false para ámbito de sesión (SET)
     */
    public static void inyectarUsuario(Connection conn, String usuario, boolean isLocal) {
        String username = (usuario != null && !usuario.trim().isEmpty()) ? usuario : SesionActual.getInstance().getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "SISTEMA";
        }
        try (PreparedStatement stmt = conn.prepareStatement("SELECT set_config('app.usuario', ?, ?)")) {
            stmt.setString(1, username);
            stmt.setBoolean(2, isLocal);
            stmt.execute();
            LOGGER.fine("app.usuario inyectado en PostgreSQL (" + username + ", is_local=" + isLocal + ")");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo inyectar app.usuario en PostgreSQL: " + e.getMessage());
        }
    }

    /**
     * Valida si es posible obtener una conexión exitosa desde el pool.
     */
    public static boolean probarConexion() {
        try (Connection conn = getConexion()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Prueba de conexión falló: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cierra el pool de conexiones HikariCP liberando todos los sockets.
     */
    public static synchronized void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Pool HikariCP cerrado correctamente.");
        }
    }
}
