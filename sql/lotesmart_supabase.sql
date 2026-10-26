-- ============================================================================
-- LOTESMART ENTERPRISE - SCRIPT DE BASE DE DATOS POSTGRESQL 15+ (SUPABASE)
-- Control de Inventario por Lotes, Regla FEFO, RBAC y Auditoría Inmutable
-- ============================================================================

-- ============================================================================
-- 0. LIMPIEZA IDEMPOTENTE (ORDEN INVERSO DE DEPENDENCIAS)
-- ============================================================================
-- Eliminar vistas
DROP VIEW IF EXISTS v_trazabilidad_lote CASCADE;
DROP VIEW IF EXISTS v_venta_resumen CASCADE;
DROP VIEW IF EXISTS v_stock_producto CASCADE;
DROP VIEW IF EXISTS v_lote_detalle CASCADE;

-- Eliminar tablas con CASCADE (elimina automáticamente FKs, índices y triggers asociados)
DROP TABLE IF EXISTS auditoria CASCADE;
DROP TABLE IF EXISTS movimiento CASCADE;
DROP TABLE IF EXISTS venta CASCADE;
DROP TABLE IF EXISTS lote CASCADE;
DROP TABLE IF EXISTS cliente CASCADE;
DROP TABLE IF EXISTS producto CASCADE;
DROP TABLE IF EXISTS proveedor CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;

-- Eliminar funciones y procedimientos almacenados
DROP PROCEDURE IF EXISTS sp_marcar_caducados() CASCADE;
DROP FUNCTION IF EXISTS fn_lote_bi() CASCADE;
DROP FUNCTION IF EXISTS fn_lote_bu() CASCADE;
DROP FUNCTION IF EXISTS fn_lote_bd() CASCADE;
DROP FUNCTION IF EXISTS fn_mov_bi() CASCADE;
DROP FUNCTION IF EXISTS fn_mov_ai() CASCADE;
DROP FUNCTION IF EXISTS fn_mov_inmutable() CASCADE;
DROP FUNCTION IF EXISTS fn_venta_bu() CASCADE;
DROP FUNCTION IF EXISTS fn_venta_bd() CASCADE;
DROP FUNCTION IF EXISTS fn_auditoria_automatica() CASCADE;

-- ============================================================================
-- 1. CREACIÓN DE TABLAS BASE (SIN DEPENDENCIAS PREVIAS)
-- ============================================================================

-- 1.1 TABLA: usuario
CREATE TABLE usuario (
    id_usuario INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(40) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'OPERADOR', 'CLIENTE')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.2 TABLA: proveedor
CREATE TABLE proveedor (
    id_proveedor INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rfc VARCHAR(13) UNIQUE NOT NULL CHECK (char_length(rfc) BETWEEN 12 AND 13),
    nombre VARCHAR(120) NOT NULL,
    telefono VARCHAR(20),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 1.3 TABLA: producto
CREATE TABLE producto (
    id_producto INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    unidad VARCHAR(20) NOT NULL,
    precio_venta NUMERIC(10,2) NOT NULL CHECK (precio_venta > 0),
    stock_minimo INT NOT NULL CHECK (stock_minimo >= 0),
    dias_minimos_venta INT NOT NULL CHECK (dias_minimos_venta >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================================
-- 2. CREACIÓN DE TABLAS DEPENDIENTES
-- ============================================================================

-- 2.1 TABLA: cliente (Depende de usuario)
CREATE TABLE cliente (
    id_cliente INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario INT UNIQUE REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    rfc VARCHAR(13) UNIQUE NOT NULL CHECK (char_length(rfc) BETWEEN 12 AND 13),
    nombre VARCHAR(120) NOT NULL,
    telefono VARCHAR(20),
    correo VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2.2 TABLA: lote (Depende de producto y proveedor)
CREATE TABLE lote (
    id_lote INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_producto INT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    id_proveedor INT NOT NULL REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT,
    codigo_lote VARCHAR(30) NOT NULL,
    fecha_caducidad DATE NOT NULL,
    cantidad_inicial INT NOT NULL CHECK (cantidad_inicial > 0),
    cantidad_disponible INT NOT NULL CHECK (cantidad_disponible >= 0 AND cantidad_disponible <= cantidad_inicial),
    costo_unitario NUMERIC(10,2) NOT NULL CHECK (costo_unitario > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE', 'AGOTADO', 'CADUCADO', 'BLOQUEADO')),
    motivo_bloqueo VARCHAR(200),
    CONSTRAINT uq_lote_producto_codigo UNIQUE (id_producto, codigo_lote)
);

-- Índice de alto desempeño para ordenamiento y despacho FEFO
CREATE INDEX ix_lote_fefo ON lote (id_producto, estado, fecha_caducidad ASC, id_lote ASC);

-- ============================================================================
-- 3. CREACIÓN DE TABLAS TRANSACCIONALES
-- ============================================================================

-- 3.1 TABLA: venta (Depende de cliente y usuario)
CREATE TABLE venta (
    id_venta INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_cliente INT NOT NULL REFERENCES cliente(id_cliente) ON DELETE RESTRICT,
    cliente VARCHAR(120) NOT NULL,
    id_usuario INT REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'CANCELADA')),
    motivo_cancelacion VARCHAR(200),
    fecha_cancelacion TIMESTAMPTZ
);

-- 3.2 TABLA: movimiento (Depende de lote y venta)
CREATE TABLE movimiento (
    id_movimiento INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_lote INT NOT NULL REFERENCES lote(id_lote) ON DELETE RESTRICT,
    id_venta INT REFERENCES venta(id_venta) ON DELETE RESTRICT,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('VENTA', 'MERMA', 'AJUSTE_NEGATIVO', 'AJUSTE_POSITIVO', 'DEVOLUCION')),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2),
    motivo VARCHAR(200),
    usuario VARCHAR(60) NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movimiento_venta CHECK (
        ((tipo IN ('VENTA', 'DEVOLUCION')) AND (id_venta IS NOT NULL)) OR
        ((tipo IN ('MERMA', 'AJUSTE_NEGATIVO', 'AJUSTE_POSITIVO')) AND (id_venta IS NULL))
    )
);

-- ============================================================================
-- 4. TABLA DE BITÁCORA INMUTABLE
-- ============================================================================

-- 4.1 TABLA: auditoria
CREATE TABLE auditoria (
    id_auditoria INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tabla VARCHAR(40) NOT NULL,
    id_registro INT NOT NULL,
    accion VARCHAR(30) NOT NULL,
    detalle VARCHAR(500) NOT NULL,
    usuario VARCHAR(60) NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 5. FUNCIONES Y TRIGGERS EN PL/PGSQL (REGLAS DE NEGOCIO EN BD)
-- ============================================================================

-- 5.1 TRIGGER: Validación al dar de alta un Lote (trg_lote_bi)
CREATE OR REPLACE FUNCTION fn_lote_bi()
RETURNS TRIGGER AS $$
DECLARE
    v_prod_activo BOOLEAN;
    v_prov_activo BOOLEAN;
    v_seed_mode VARCHAR;
BEGIN
    v_seed_mode := COALESCE(NULLIF(current_setting('app.seed_mode', true), ''), 'false');

    -- Validar fecha de caducidad posterior a hoy (excepto en carga inicial seed de prueba o estado CADUCADO)
    IF NEW.fecha_caducidad <= CURRENT_DATE AND NEW.estado != 'CADUCADO' AND v_seed_mode != 'true' THEN
        RAISE EXCEPTION 'La fecha de caducidad debe ser posterior a la fecha actual';
    END IF;

    -- Validar producto activo
    SELECT activo INTO v_prod_activo FROM producto WHERE id_producto = NEW.id_producto;
    IF v_prod_activo IS NULL OR v_prod_activo = FALSE THEN
        RAISE EXCEPTION 'El producto seleccionado no existe o no se encuentra activo';
    END IF;

    -- Validar proveedor activo
    SELECT activo INTO v_prov_activo FROM proveedor WHERE id_proveedor = NEW.id_proveedor;
    IF v_prov_activo IS NULL OR v_prov_activo = FALSE THEN
        RAISE EXCEPTION 'El proveedor seleccionado no existe o no se encuentra activo';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lote_bi
BEFORE INSERT ON lote
FOR EACH ROW EXECUTE FUNCTION fn_lote_bi();

-- 5.2 TRIGGER: Protección de existencia e inmutabilidad en Lote (trg_lote_bu)
CREATE OR REPLACE FUNCTION fn_lote_bu()
RETURNS TRIGGER AS $$
DECLARE
    v_internal VARCHAR;
    v_tiene_movimientos BOOLEAN;
BEGIN
    v_internal := COALESCE(NULLIF(current_setting('app.internal_mov', true), ''), 'false');

    -- Prohibir actualizaciones directas a cantidad_disponible sin movimiento oficial
    IF OLD.cantidad_disponible IS DISTINCT FROM NEW.cantidad_disponible AND v_internal != 'true' THEN
        RAISE EXCEPTION 'No se permite actualizar directamente la cantidad_disponible del lote. Debe registrar un movimiento (VENTA, MERMA, AJUSTE, DEVOLUCION).';
    END IF;

    -- Inmutabilidad de datos originales si ya existen movimientos registrados
    SELECT EXISTS (SELECT 1 FROM movimiento WHERE id_lote = OLD.id_lote) INTO v_tiene_movimientos;
    IF v_tiene_movimientos THEN
        IF OLD.cantidad_inicial IS DISTINCT FROM NEW.cantidad_inicial OR
           OLD.costo_unitario IS DISTINCT FROM NEW.costo_unitario OR
           OLD.id_producto IS DISTINCT FROM NEW.id_producto THEN
            RAISE EXCEPTION 'No se puede modificar cantidad inicial, costo unitario ni producto de un lote que ya cuenta con movimientos registrados.';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lote_bu
BEFORE UPDATE ON lote
FOR EACH ROW EXECUTE FUNCTION fn_lote_bu();

-- 5.3 TRIGGER: Prohibir eliminación de Lote si tiene movimientos (trg_lote_bd)
CREATE OR REPLACE FUNCTION fn_lote_bd()
RETURNS TRIGGER AS $$
DECLARE
    v_tiene_movimientos BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1 FROM movimiento WHERE id_lote = OLD.id_lote) INTO v_tiene_movimientos;
    IF v_tiene_movimientos THEN
        RAISE EXCEPTION 'No se puede eliminar un lote que ya cuenta con movimientos registrados en el sistema.';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lote_bd
BEFORE DELETE ON lote
FOR EACH ROW EXECUTE FUNCTION fn_lote_bd();

-- 5.4 TRIGGER: Validación previa a la inserción de Movimientos (trg_mov_bi)
CREATE OR REPLACE FUNCTION fn_mov_bi()
RETURNS TRIGGER AS $$
DECLARE
    v_estado VARCHAR;
    v_caducidad DATE;
    v_disp INT;
    v_inicial INT;
BEGIN
    -- Capturar usuario de auditoría desde la sesión de la aplicación
    NEW.usuario := COALESCE(NULLIF(current_setting('app.usuario', true), ''), current_user);

    -- Consultar datos actuales del lote
    SELECT estado, fecha_caducidad, cantidad_disponible, cantidad_inicial
    INTO v_estado, v_caducidad, v_disp, v_inicial
    FROM lote WHERE id_lote = NEW.id_lote;

    IF v_estado IS NULL THEN
        RAISE EXCEPTION 'El lote con ID % no existe', NEW.id_lote;
    END IF;

    -- Validaciones según tipo de movimiento
    IF NEW.tipo = 'VENTA' THEN
        IF v_estado != 'VIGENTE' THEN
            RAISE EXCEPTION 'El lote % no está en estado VIGENTE (Estado actual: %)', NEW.id_lote, v_estado;
        END IF;
        IF v_caducidad < CURRENT_DATE THEN
            RAISE EXCEPTION 'El lote % se encuentra vencido desde %', NEW.id_lote, v_caducidad;
        END IF;
        IF v_disp < NEW.cantidad THEN
            RAISE EXCEPTION 'Existencia insuficiente en lote %. Disponible: %, Solicitada: %', NEW.id_lote, v_disp, NEW.cantidad;
        END IF;
    ELSIF NEW.tipo IN ('MERMA', 'AJUSTE_NEGATIVO') THEN
        IF NEW.motivo IS NULL OR TRIM(NEW.motivo) = '' THEN
            RAISE EXCEPTION 'El motivo es obligatorio para registrar %', NEW.tipo;
        END IF;
        IF v_disp < NEW.cantidad THEN
            RAISE EXCEPTION 'Existencia insuficiente en lote % para aplicar %. Disponible: %, Solicitada: %', NEW.id_lote, NEW.tipo, v_disp, NEW.cantidad;
        END IF;
    ELSIF NEW.tipo = 'AJUSTE_POSITIVO' THEN
        IF NEW.motivo IS NULL OR TRIM(NEW.motivo) = '' THEN
            RAISE EXCEPTION 'El motivo es obligatorio para registrar AJUSTE_POSITIVO';
        END IF;
        IF (v_disp + NEW.cantidad) > v_inicial THEN
            RAISE EXCEPTION 'El ajuste positivo excede la cantidad inicial del lote (Inicial: %, Actual: %, Suma: %)', v_inicial, v_disp, (v_disp + NEW.cantidad);
        END IF;
    ELSIF NEW.tipo = 'DEVOLUCION' THEN
        IF (v_disp + NEW.cantidad) > v_inicial THEN
            RAISE EXCEPTION 'La devolución excede la cantidad inicial del lote (Inicial: %, Actual: %, Suma: %)', v_inicial, v_disp, (v_disp + NEW.cantidad);
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_mov_bi
BEFORE INSERT ON movimiento
FOR EACH ROW EXECUTE FUNCTION fn_mov_bi();

-- 5.5 TRIGGER: Actualización automática de existencias en Lote (trg_mov_ai)
CREATE OR REPLACE FUNCTION fn_mov_ai()
RETURNS TRIGGER AS $$
BEGIN
    -- Habilitar permiso para modificar cantidad_disponible internamente
    PERFORM set_config('app.internal_mov', 'true', true);

    IF NEW.tipo IN ('VENTA', 'MERMA', 'AJUSTE_NEGATIVO') THEN
        UPDATE lote
        SET cantidad_disponible = cantidad_disponible - NEW.cantidad,
            estado = CASE 
                WHEN (cantidad_disponible - NEW.cantidad) = 0 THEN 'AGOTADO'
                ELSE estado 
            END
        WHERE id_lote = NEW.id_lote;
    ELSIF NEW.tipo IN ('AJUSTE_POSITIVO', 'DEVOLUCION') THEN
        UPDATE lote
        SET cantidad_disponible = cantidad_disponible + NEW.cantidad,
            estado = CASE 
                WHEN estado = 'AGOTADO' AND fecha_caducidad >= CURRENT_DATE AND motivo_bloqueo IS NULL THEN 'VIGENTE'
                ELSE estado 
            END
        WHERE id_lote = NEW.id_lote;
    END IF;

    -- Restaurar variable de sesión interna
    PERFORM set_config('app.internal_mov', 'false', true);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_mov_ai
AFTER INSERT ON movimiento
FOR EACH ROW EXECUTE FUNCTION fn_mov_ai();

-- 5.6 TRIGGER: Inmutabilidad estricta de Movimientos (trg_mov_bu, trg_mov_bd)
CREATE OR REPLACE FUNCTION fn_mov_inmutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de movimiento son inmutables. No se permite actualización ni eliminación.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_mov_bu
BEFORE UPDATE ON movimiento
FOR EACH ROW EXECUTE FUNCTION fn_mov_inmutable();

CREATE TRIGGER trg_mov_bd
BEFORE DELETE ON movimiento
FOR EACH ROW EXECUTE FUNCTION fn_mov_inmutable();

-- 5.7 TRIGGER: Reglas de Venta y Cancelación (trg_venta_bu, trg_venta_bd)
CREATE OR REPLACE FUNCTION fn_venta_bu()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado = 'CANCELADA' THEN
        RAISE EXCEPTION 'No se puede modificar una venta que ya ha sido cancelada.';
    END IF;

    IF OLD.estado = 'REGISTRADA' AND NEW.estado = 'CANCELADA' THEN
        IF NEW.motivo_cancelacion IS NULL OR TRIM(NEW.motivo_cancelacion) = '' THEN
            RAISE EXCEPTION 'Es obligatorio proporcionar el motivo de cancelación de la venta.';
        END IF;
        NEW.fecha_cancelacion := CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venta_bu
BEFORE UPDATE ON venta
FOR EACH ROW EXECUTE FUNCTION fn_venta_bu();

CREATE OR REPLACE FUNCTION fn_venta_bd()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'No se permite eliminar ventas. Las operaciones deben cancelarse formalmente a través del sistema.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venta_bd
BEFORE DELETE ON venta
FOR EACH ROW EXECUTE FUNCTION fn_venta_bd();

-- 5.8 TRIGGER GENÉRICO DE AUDITORÍA AUTOMÁTICA
CREATE OR REPLACE FUNCTION fn_auditoria_automatica()
RETURNS TRIGGER AS $$
DECLARE
    v_user VARCHAR;
    v_id INT;
    v_detalle VARCHAR(500);
BEGIN
    v_user := COALESCE(NULLIF(current_setting('app.usuario', true), ''), current_user);

    IF TG_OP = 'INSERT' THEN
        EXECUTE 'SELECT ($1).' || quote_ident(TG_ARGV[0]) INTO v_id USING NEW;
        v_detalle := 'Inserción de registro en ' || TG_TABLE_NAME || ' con ID ' || v_id;
    ELSIF TG_OP = 'UPDATE' THEN
        EXECUTE 'SELECT ($1).' || quote_ident(TG_ARGV[0]) INTO v_id USING NEW;
        v_detalle := 'Actualización de registro en ' || TG_TABLE_NAME || ' con ID ' || v_id;
        IF TG_TABLE_NAME = 'lote' THEN
            v_detalle := v_detalle || ' [Estado: ' || OLD.estado || ' -> ' || NEW.estado || ', Disp: ' || OLD.cantidad_disponible || ' -> ' || NEW.cantidad_disponible || ']';
        ELSIF TG_TABLE_NAME = 'venta' THEN
            v_detalle := v_detalle || ' [Estado: ' || OLD.estado || ' -> ' || NEW.estado || ']';
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        EXECUTE 'SELECT ($1).' || quote_ident(TG_ARGV[0]) INTO v_id USING OLD;
        v_detalle := 'Eliminación de registro en ' || TG_TABLE_NAME || ' con ID ' || v_id;
    END IF;

    INSERT INTO auditoria (tabla, id_registro, accion, detalle, usuario, fecha)
    VALUES (TG_TABLE_NAME, COALESCE(v_id, 0), TG_OP, SUBSTRING(v_detalle FROM 1 FOR 500), v_user, CURRENT_TIMESTAMP);

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_usuario AFTER INSERT OR UPDATE OR DELETE ON usuario FOR EACH ROW EXECUTE FUNCTION fn_auditoria_automatica('id_usuario');
CREATE TRIGGER trg_auditoria_proveedor AFTER INSERT OR UPDATE OR DELETE ON proveedor FOR EACH ROW EXECUTE FUNCTION fn_auditoria_automatica('id_proveedor');
CREATE TRIGGER trg_auditoria_producto AFTER INSERT OR UPDATE OR DELETE ON producto FOR EACH ROW EXECUTE FUNCTION fn_auditoria_automatica('id_producto');
CREATE TRIGGER trg_auditoria_cliente AFTER INSERT OR UPDATE OR DELETE ON cliente FOR EACH ROW EXECUTE FUNCTION fn_auditoria_automatica('id_cliente');
CREATE TRIGGER trg_auditoria_lote AFTER INSERT OR UPDATE OR DELETE ON lote FOR EACH ROW EXECUTE FUNCTION fn_auditoria_automatica('id_lote');
CREATE TRIGGER trg_auditoria_venta AFTER INSERT OR UPDATE OR DELETE ON venta FOR EACH ROW EXECUTE FUNCTION fn_auditoria_automatica('id_venta');

-- 5.9 PROCEDIMIENTO ALMACENADO: sp_marcar_caducados
CREATE OR REPLACE PROCEDURE sp_marcar_caducados()
AS $$
DECLARE
    r RECORD;
    v_user VARCHAR;
BEGIN
    v_user := COALESCE(NULLIF(current_setting('app.usuario', true), ''), 'SISTEMA');

    FOR r IN 
        SELECT id_lote, codigo_lote, fecha_caducidad 
        FROM lote 
        WHERE estado = 'VIGENTE' AND fecha_caducidad < CURRENT_DATE
    LOOP
        UPDATE lote SET estado = 'CADUCADO' WHERE id_lote = r.id_lote;

        INSERT INTO auditoria (tabla, id_registro, accion, detalle, usuario, fecha)
        VALUES ('lote', r.id_lote, 'CADUCIDAD_AUTO', 
                'Lote ' || r.codigo_lote || ' marcado como CADUCADO automáticamente (Fecha: ' || r.fecha_caducidad || ')',
                v_user, CURRENT_TIMESTAMP);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 6. VISTAS DE NEGOCIO
-- ============================================================================

-- 6.1 VISTA: v_lote_detalle
CREATE OR REPLACE VIEW v_lote_detalle AS
SELECT 
    l.id_lote,
    l.id_producto,
    p.sku,
    p.nombre AS nombre_producto,
    p.unidad,
    p.dias_minimos_venta,
    l.id_proveedor,
    pr.nombre AS nombre_proveedor,
    l.codigo_lote,
    l.fecha_caducidad,
    l.cantidad_inicial,
    l.cantidad_disponible,
    l.costo_unitario,
    (l.cantidad_disponible * l.costo_unitario) AS valor_existencia,
    l.estado,
    l.motivo_bloqueo,
    (l.fecha_caducidad - CURRENT_DATE) AS dias_restantes,
    CASE 
        WHEN l.estado = 'BLOQUEADO' THEN 'BLOQUEADO'
        WHEN l.estado = 'CADUCADO' OR (l.fecha_caducidad < CURRENT_DATE) THEN 'CADUCADO'
        WHEN l.cantidad_disponible = 0 OR l.estado = 'AGOTADO' THEN 'AGOTADO'
        WHEN (l.fecha_caducidad - CURRENT_DATE) < 30 THEN 'ROJO'
        WHEN (l.fecha_caducidad - CURRENT_DATE) <= 60 THEN 'AMARILLO'
        ELSE 'VERDE'
    END AS semaforo
FROM lote l
JOIN producto p ON l.id_producto = p.id_producto
JOIN proveedor pr ON l.id_proveedor = pr.id_proveedor;

-- 6.2 VISTA: v_stock_producto
CREATE OR REPLACE VIEW v_stock_producto AS
SELECT 
    p.id_producto,
    p.sku,
    p.nombre,
    p.unidad,
    p.precio_venta,
    p.stock_minimo,
    p.dias_minimos_venta,
    p.activo,
    COALESCE(SUM(CASE 
        WHEN l.estado = 'VIGENTE' AND (l.fecha_caducidad - CURRENT_DATE) >= p.dias_minimos_venta 
        THEN l.cantidad_disponible ELSE 0 END), 0) AS stock_vigente,
    COALESCE(SUM(CASE 
        WHEN l.cantidad_disponible > 0 AND (l.estado != 'VIGENTE' OR (l.fecha_caducidad - CURRENT_DATE) < p.dias_minimos_venta)
        THEN l.cantidad_disponible ELSE 0 END), 0) AS stock_no_vendible,
    COALESCE(SUM(l.cantidad_disponible), 0) AS stock_total,
    (COALESCE(SUM(CASE 
        WHEN l.estado = 'VIGENTE' AND (l.fecha_caducidad - CURRENT_DATE) >= p.dias_minimos_venta 
        THEN l.cantidad_disponible ELSE 0 END), 0) < p.stock_minimo) AS alerta_reorden
FROM producto p
LEFT JOIN lote l ON p.id_producto = l.id_producto
GROUP BY p.id_producto, p.sku, p.nombre, p.unidad, p.precio_venta, p.stock_minimo, p.dias_minimos_venta, p.activo;

-- 6.3 VISTA: v_venta_resumen
CREATE OR REPLACE VIEW v_venta_resumen AS
SELECT 
    v.id_venta,
    ('V-' || LPAD(v.id_venta::text, 6, '0')) AS folio,
    v.fecha,
    v.id_cliente,
    v.cliente,
    v.id_usuario,
    u.username,
    v.total,
    v.estado,
    v.motivo_cancelacion,
    v.fecha_cancelacion,
    COALESCE(SUM(CASE 
        WHEN m.tipo = 'VENTA' THEN m.cantidad * l.costo_unitario 
        WHEN m.tipo = 'DEVOLUCION' THEN - (m.cantidad * l.costo_unitario)
        ELSE 0 END), 0) AS costo_total,
    (v.total - COALESCE(SUM(CASE 
        WHEN m.tipo = 'VENTA' THEN m.cantidad * l.costo_unitario 
        WHEN m.tipo = 'DEVOLUCION' THEN - (m.cantidad * l.costo_unitario)
        ELSE 0 END), 0)) AS margen_bruto
FROM venta v
LEFT JOIN usuario u ON v.id_usuario = u.id_usuario
LEFT JOIN movimiento m ON v.id_venta = m.id_venta
LEFT JOIN lote l ON m.id_lote = l.id_lote
GROUP BY v.id_venta, v.fecha, v.id_cliente, v.cliente, v.id_usuario, u.username, v.total, v.estado, v.motivo_cancelacion, v.fecha_cancelacion;

-- 6.4 VISTA: v_trazabilidad_lote
CREATE OR REPLACE VIEW v_trazabilidad_lote AS
SELECT 
    l.id_lote,
    l.codigo_lote,
    l.fecha_caducidad,
    p.id_producto,
    p.sku,
    p.nombre AS nombre_producto,
    m.id_movimiento,
    v.id_venta,
    ('V-' || LPAD(v.id_venta::text, 6, '0')) AS folio_venta,
    v.fecha AS fecha_venta,
    c.id_cliente,
    c.nombre AS nombre_cliente,
    c.telefono AS telefono_cliente,
    c.correo AS correo_cliente,
    m.cantidad AS cantidad_entregada
FROM movimiento m
JOIN lote l ON m.id_lote = l.id_lote
JOIN producto p ON l.id_producto = p.id_producto
JOIN venta v ON m.id_venta = v.id_venta
JOIN cliente c ON v.id_cliente = c.id_cliente
WHERE m.tipo = 'VENTA';

-- ============================================================================
-- 7. DATOS DE PRUEBA INICIALES (SEED DATA EN ORDEN ESTRICTO)
-- ============================================================================
-- Hash SHA-256 de las contraseñas:
-- admin123    -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- operador123 -> 1725165c9a0b3698a3d01016e0d8205155820b8d7f21835ca64c0f81c728d880
-- cliente123  -> 09a31a7001e261ab1e056182a71d3cf57f582ca9a29cff5eb83be0f0549730a9

-- 7.1 INSERCIÓN: usuario
INSERT INTO usuario (username, password_hash, nombre_completo, rol, activo)
VALUES 
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Administrador General', 'ADMIN', TRUE),
('operador', '1725165c9a0b3698a3d01016e0d8205155820b8d7f21835ca64c0f81c728d880', 'Juan Almacenista', 'OPERADOR', TRUE),
('esperanza', '09a31a7001e261ab1e056182a71d3cf57f582ca9a29cff5eb83be0f0549730a9', 'Tienda La Esperanza Portal', 'CLIENTE', TRUE),
('sanangel', '09a31a7001e261ab1e056182a71d3cf57f582ca9a29cff5eb83be0f0549730a9', 'Farmacia San Ángel Portal', 'CLIENTE', TRUE);

-- 7.2 INSERCIÓN: proveedor
INSERT INTO proveedor (rfc, nombre, telefono, activo)
VALUES 
('LBAJ800101GH4', 'Lácteos del Bajío S.A. de C.V.', '4771234567', TRUE),
('LFPI750220IJ5', 'Laboratorios Farmacéuticos Pisa S.A.', '3339876543', TRUE),
('ASME910404MN7', 'Alimentos y Soluciones Médicas S.A.', '5553344556', TRUE);

-- 7.3 INSERCIÓN: producto
INSERT INTO producto (sku, nombre, unidad, precio_venta, stock_minimo, dias_minimos_venta, activo)
VALUES 
('LEC-1001', 'Leche entera 1 L', 'LITRO', 26.50, 50, 7, TRUE),
('MED-2001', 'Paracetamol 500mg', 'CAJA', 45.00, 30, 30, TRUE),
('SUE-3001', 'Suero Oral 500ml', 'FRASCO', 18.00, 20, 15, TRUE),
('YOG-1002', 'Yogurt Natural 1 Kg', 'KILO', 38.00, 15, 10, TRUE);

-- 7.4 INSERCIÓN: cliente (Enlaza con los usuarios creados en 7.1)
INSERT INTO cliente (id_usuario, rfc, nombre, telefono, correo, activo)
VALUES 
((SELECT id_usuario FROM usuario WHERE username = 'esperanza'), 'TESP900101AB1', 'Tienda La Esperanza S.A.', '5551234567', 'contacto@tiendelaesperanza.com', TRUE),
((SELECT id_usuario FROM usuario WHERE username = 'sanangel'), 'FSAN850512CD2', 'Farmacia San Ángel', '5557654321', 'ventas@farmaciasanangel.com', TRUE),
(NULL, 'DMED980315EF3', 'Distribuidora Médica del Norte', '8182233445', 'pedidos@dmednorte.com', TRUE);

-- 7.5 INSERCIÓN: lote (Habilitando modo semilla temporal para admitir lote de prueba vencido)
SET LOCAL app.seed_mode = 'true';

-- Producto 1: Leche entera 1 L (LEC-1001)
-- Lote LG-2410-A: vence en 14 días (35 unidades)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'LEC-1001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'LBAJ800101GH4'),
    'LG-2410-A', CURRENT_DATE + 14, 35, 35, 18.50, 'VIGENTE'
);

-- Lote LG-2410-B: vence en 20 días (50 unidades)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'LEC-1001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'LBAJ800101GH4'),
    'LG-2410-B', CURRENT_DATE + 20, 50, 50, 19.00, 'VIGENTE'
);

-- Lote LG-2411-C: vence en 45 días (100 unidades)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'LEC-1001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'LBAJ800101GH4'),
    'LG-2411-C', CURRENT_DATE + 45, 100, 100, 19.50, 'VIGENTE'
);

-- Producto 2: Paracetamol 500mg (MED-2001) - Requiere 30 días mínimos de venta
-- Lote PA-77812: vence en 25 días (25 < 30 días mínimos: no vendible por política comercial)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'MED-2001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'LFPI750220IJ5'),
    'PA-77812', CURRENT_DATE + 25, 40, 40, 22.00, 'VIGENTE'
);

-- Lote PA-88901: vence en 90 días (vendible)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'MED-2001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'LFPI750220IJ5'),
    'PA-88901', CURRENT_DATE + 90, 80, 80, 24.00, 'VIGENTE'
);

-- Producto 3: Suero Oral 500ml (SUE-3001)
-- Lote SO-4410: venció hace 5 días (CADUCADO)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'SUE-3001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'ASME910404MN7'),
    'SO-4410', CURRENT_DATE - 5, 20, 20, 10.00, 'CADUCADO'
);

-- Lote SO-5520: vence en 60 días (VIGENTE)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'SUE-3001'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'ASME910404MN7'),
    'SO-5520', CURRENT_DATE + 60, 50, 50, 10.50, 'VIGENTE'
);

-- Producto 4: Yogurt Natural 1 Kg (YOG-1002)
-- Lote YO-1105: vence en 15 días (VIGENTE)
INSERT INTO lote (id_producto, id_proveedor, codigo_lote, fecha_caducidad, cantidad_inicial, cantidad_disponible, costo_unitario, estado)
VALUES (
    (SELECT id_producto FROM producto WHERE sku = 'YOG-1002'),
    (SELECT id_proveedor FROM proveedor WHERE rfc = 'LBAJ800101GH4'),
    'YO-1105', CURRENT_DATE + 15, 30, 30, 22.00, 'VIGENTE'
);

SET LOCAL app.seed_mode = 'false';
