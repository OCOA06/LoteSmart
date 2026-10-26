# LoteSmart Enterprise - Sistema de Control de Inventario por Lotes FEFO

Sistema empresarial completo para la gestión y control de inventario por lotes con fecha de caducidad, despacho automático bajo la regla **FEFO (First Expired, First Out)**, control de acceso basado en 3 roles (RBAC), interfaz gráfica defensiva (Poka-Yoke) desarrollada en **Java Swing moderno (FlatLaf)** y persistencia en la nube sobre **PostgreSQL (Supabase)**.

---

## 1. Arquitectura y Tecnologías

El proyecto está construido bajo **Apache Maven** y una arquitectura estricta en 5 capas desacopladas:

```
src/main/java/com/lotesmart/
├── modelo/          # Entidades POJO, Enums y SesionActual (Singleton)
├── datos/           # Acceso JDBC con PreparedStatement, HikariCP y traducción SQLState
├── negocio/         # FefoAllocator puro en memoria, VentaService transaccional y AuthService
├── reportes/        # Motor reporteador ejecutivo en PDF (OpenPDF) y Excel (Apache POI)
└── vista/           # UI Swing FlatLaf con diseño Poka-Yoke (LGoodDatePicker, JSpinner, DocumentFilter)
```

### Dependencias Principales (`pom.xml`)
- **Java SE:** JDK 17+
- **PostgreSQL JDBC Driver:** `42.7.4`
- **HikariCP:** `5.1.0` (Pool optimizado con SSL hacia Supabase)
- **FlatLaf:** `3.5.1` (Look and Feel moderno de alto contraste con bordes redondeados)
- **LGoodDatePicker:** `11.2.1` (Selector de fechas con veto policy Poka-Yoke)
- **OpenPDF (LibrePDF):** `2.0.2` (Generación de tickets y órdenes de retiro en PDF)
- **Apache POI:** `5.3.0` (Exportación financiera, semáforo y kardex a `.xlsx`)
- **JUnit 5 Jupiter:** `5.10.3` (Pruebas unitarias del algoritmo FEFO y seguridad)

---

## 2. Base de Datos PostgreSQL / Supabase (`sql/lotesmart_supabase.sql`)

El script SQL es idempotente y contiene:

1. **Tablas con Integridad Referencial:**
   - `usuario`: Credenciales con hash SHA-256 y roles (`ADMIN`, `OPERADOR`, `CLIENTE`).
   - `cliente`: RFC de 12 o 13 caracteres, vinculado a usuario para portal B2B.
   - `proveedor`: RFC de 12 o 13 caracteres y estado activo.
   - `producto`: SKU único, precio de venta, stock mínimo y días mínimos de vida útil requeridos para venta.
   - `lote`: Código único por producto, fecha de caducidad, cantidad inicial/disponible, costo y estado (`VIGENTE`, `AGOTADO`, `CADUCADO`, `BLOQUEADO`). Índice FEFO: `CREATE INDEX ix_lote_fefo ON lote (id_producto, estado, fecha_caducidad ASC, id_lote ASC)`.
   - `venta`: Folio `V-00000X`, cliente, usuario, total y estado (`REGISTRADA`, `CANCELADA`).
   - `movimiento`: Histórico inmutable de transacciones (`VENTA`, `MERMA`, `AJUSTE_NEGATIVO`, `AJUSTE_POSITIVO`, `DEVOLUCION`).
   - `auditoria`: Bitácora inmutable de cambios disparada por triggers.

2. **Triggers PL/pgSQL y Reglas de Negocio:**
   - `trg_lote_bi`: Impide dar de alta lotes con fecha vencida o productos/proveedores inactivos.
   - `trg_lote_bu` & `trg_lote_bd`: Prohíbe actualizar `cantidad_disponible` directamente (solo por movimientos) e impide eliminar o modificar datos iniciales si el lote ya tiene movimientos registrados.
   - `trg_mov_bi` & `trg_mov_ai`: Valida estado vigente y existencia antes de insertar movimientos; actualiza automáticamente existencias y estado (`AGOTADO` al llegar a 0; `VIGENTE` al repor existencias si no está caducado/bloqueado).
   - `trg_mov_bu` & `trg_mov_bd`: Prohíbe modificaciones y eliminaciones en la tabla de movimientos (inmutabilidad estricta).
   - `trg_venta_bu` & `trg_venta_bd`: Prohíbe borrar ventas; exige motivo formal para cancelar y fecha de cancelación.
   - `trg_auditoria_*`: Auditoría automática en todas las tablas capturando usuario de sesión `current_setting('app.usuario', true)`.
   - `sp_marcar_caducados()`: Procedimiento que marca automáticamente lotes vencidos al iniciar el sistema.

3. **Vistas de Negocio:**
   - `v_lote_detalle`: Semáforo dinámico (`ROJO` < 30 días, `AMARILLO` 30-60 días, `VERDE` > 60 días, `CADUCADO`, `BLOQUEADO`, `AGOTADO`) y valor monetario en existencia.
   - `v_stock_producto`: Existencia vendible según días mínimos, stock no vendible y alerta booleana de reorden.
   - `v_venta_resumen`: Ventas con costo real de lotes y margen bruto monetario (`total - costo_lotes`).
   - `v_trazabilidad_lote`: Relación lote -> clientes -> folios de venta para retiro de mercado.

---

## 3. Matriz de Roles y Permisos (RBAC)

| Módulo / Funcionalidad | ADMIN | OPERADOR | CLIENTE (B2B) |
| :--- | :---: | :---: | :---: |
| **Tablero de Control (KPIs, Dinero en Riesgo)** | Sí | Sí | No |
| **Punto de Venta FEFO (Carrito y Simulación)** | Sí | Sí | No |
| **Cancelar Ventas (Devolución a lotes)** | Sí | No | No |
| **Recepción de Nuevos Lotes** | Sí | Sí | No |
| **Bloqueo Sanitario Preventivo** | Sí | Sí | No |
| **Ajustes de Inventario y Mermas** | Sí | No | No |
| **Eliminar Lotes sin Movimientos** | Sí | No | No |
| **Catálogos (Productos, Proveedores, Clientes, Usuarios)** | Sí | No | No |
| **Trazabilidad y Retiro de Mercado Sanitario** | Sí | Sí | No |
| **Bitácora Inmutable de Auditoría** | Sí | No | No |
| **Reportes de Margen Bruto Financiero** | Sí | No | No |
| **Reportes Semáforo y Tickets de Venta** | Sí | Sí | No |
| **Portal B2B Autopedido con Simulación FEFO** | No | No | Sí |
| **Mis Compras y Certificado de Lote en PDF** | No | No | Sí |

---

## 4. Usuarios de Prueba (Seed Data)

Las contraseñas se encuentran hasheadas con SHA-256 en la base de datos y cuentan con botones de acceso rápido en el `LoginFrame`:

1. **Administrador General:**
   - **Usuario:** `admin`
   - **Contraseña:** `admin123`
   - **Rol:** `ADMIN`
2. **Operador de Almacén:**
   - **Usuario:** `operador`
   - **Contraseña:** `operador123`
   - **Rol:** `OPERADOR`
3. **Cliente B2B - Tienda La Esperanza:**
   - **Usuario:** `esperanza`
   - **Contraseña:** `cliente123`
   - **Rol:** `CLIENTE`
4. **Cliente B2B - Farmacia San Ángel:**
   - **Usuario:** `sanangel`
   - **Contraseña:** `cliente123`
   - **Rol:** `CLIENTE`

---

## 5. Compilación y Ejecución

### Arranque Automático en Windows (Recomendado)
Doble clic o ejecución desde terminal en:
```cmd
ejecutar.bat
```
Este script realiza automáticamente:
1. `call mvn clean compile` (Compilación limpia del código fuente).
2. `call mvn test` (Ejecución de todas las pruebas unitarias: FEFO, seguridad, configuración).
3. `call mvn exec:java -Dexec.mainClass="com.lotesmart.Main"` (Arranque de la aplicación Swing).

O para arranque ultrarrápido desde el Fat JAR:
```cmd
ejecutar_jar.bat
```

### Comandos Manuales de Terminal
```bash
mvn test
mvn package
mvn exec:java
java -jar target/lotesmart-enterprise-1.0.0.jar
```

---

## 6. Configuración de Base de Datos (`db.properties`)

El archivo `db.properties` en la raíz del proyecto permite configurar la conexión hacia Supabase Cloud o cualquier PostgreSQL 15+:

```properties
db.url=jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require
db.user=postgres.TU_PROJECT_REF
db.password=TU_PASSWORD_AQUI
```

También es posible modificar los parámetros en tiempo de ejecución haciendo clic en el botón **"Configurar BD"** en la pantalla de inicio de sesión.
