-- =============================================================================
-- callsos-bd  —  Schema completo
-- Versión: Fase 3
-- Nota: los scripts en /docker-entrypoint-initdb.d se ejecutan en orden
--       alfabético. Este archivo se llama schema.sql para ejecutarse antes
--       que data.sql.
-- =============================================================================

-- Asegurar encoding correcto
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- =============================================================================
-- TABLA: unidades_policiales  (antes cais_cartagena)
-- Representa los CAIs y el Comando.
-- Unificamos el nombre con el adaptador JDBC: UnidadPolicialRepositoryMySQL
-- consulta la tabla 'unidades_policiales'.
-- =============================================================================
CREATE TABLE IF NOT EXISTS unidades_policiales (
    id          VARCHAR(36)  NOT NULL DEFAULT (UUID()),
    nombre      VARCHAR(100) NOT NULL,
    direccion   VARCHAR(255) NOT NULL,
    latitud     DECIMAL(10,8) NOT NULL,
    longitud    DECIMAL(11,8) NOT NULL,
    telefono    VARCHAR(20)  NULL,         -- agregado: requerido por el RowMapper
    token_fcm   VARCHAR(255) NULL,         -- Épica 5: notificaciones push al CAI
    PRIMARY KEY (id),
    -- Índice espacial aproximado para consultas de cercanía (Haversine)
    INDEX idx_ubicacion (latitud, longitud)
);

-- =============================================================================
-- TABLA: denunciantes
-- Ciudadanos que reportan incidentes.
-- Incluye token_fcm para notificaciones push (Firebase Cloud Messaging).
-- =============================================================================
CREATE TABLE IF NOT EXISTS denunciantes (
    id          VARCHAR(36)  NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    origen      VARCHAR(255) NULL,
    telefono    VARCHAR(20)  NULL,
    correo      VARCHAR(100) NULL,
    token_fcm   VARCHAR(255) NULL,         -- requerido para notificaciones push
    PRIMARY KEY (id)
);

-- =============================================================================
-- TABLA: agentes
-- Policías operativos. Pertenecen a una UnidadPolicial.
-- latitud/longitud: última posición conocida del agente (actualizable).
-- =============================================================================
CREATE TABLE IF NOT EXISTS agentes (
    id                   VARCHAR(36)  NOT NULL,
    nombre               VARCHAR(100) NOT NULL,
    direccion            VARCHAR(255) NULL,
    latitud              DECIMAL(10,8) NULL,
    longitud             DECIMAL(11,8) NULL,
    telefono             VARCHAR(20)  NULL,
    estado               VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    unidad_policial_id   VARCHAR(36)  NOT NULL,
    token_fcm            VARCHAR(255) NULL,         -- Épica 5: notificaciones push al agente
    PRIMARY KEY (id),
    INDEX idx_estado (estado),
    INDEX idx_unidad (unidad_policial_id),
    CONSTRAINT fk_agente_unidad
        FOREIGN KEY (unidad_policial_id)
        REFERENCES unidades_policiales(id)
);

-- =============================================================================
-- TABLA: incidentes
-- Agregado raíz del dominio.
-- unidad_policial_id: se llena cuando el Comando deriva el incidente a un CAI.
-- =============================================================================
CREATE TABLE IF NOT EXISTS incidentes (
    id                   VARCHAR(36)   NOT NULL,
    fecha_hora           DATETIME      NOT NULL,
    tipo                 VARCHAR(50)   NOT NULL,
    descripcion          TEXT          NULL,
    estado               VARCHAR(30)   NOT NULL DEFAULT 'CREADO',
    latitud              DECIMAL(10,8) NOT NULL,
    longitud             DECIMAL(11,8) NOT NULL,
    denunciante_id       VARCHAR(36)   NOT NULL,
    unidad_policial_id   VARCHAR(36)   NULL,     -- NULL hasta ser derivado al CAI
    PRIMARY KEY (id),
    INDEX idx_estado      (estado),
    INDEX idx_denunciante (denunciante_id),
    INDEX idx_unidad      (unidad_policial_id),
    INDEX idx_ubicacion   (latitud, longitud),
    CONSTRAINT fk_incidente_denunciante
        FOREIGN KEY (denunciante_id)
        REFERENCES denunciantes(id),
    CONSTRAINT fk_incidente_unidad
        FOREIGN KEY (unidad_policial_id)
        REFERENCES unidades_policiales(id)
);

-- =============================================================================
-- TABLA: denuncias
-- Registro formal de la denuncia. Nace del Denunciante + Incidente.
-- =============================================================================
CREATE TABLE IF NOT EXISTS denuncias (
    id              VARCHAR(36)   NOT NULL,
    fecha           DATETIME      NOT NULL,
    tipo            VARCHAR(50)   NOT NULL,
    descripcion     TEXT          NULL,
    latitud         DECIMAL(10,8) NULL,
    longitud        DECIMAL(11,8) NULL,
    denunciante_id  VARCHAR(36)   NOT NULL,
    incidente_id    VARCHAR(36)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_incidente   (incidente_id),
    INDEX idx_denunciante (denunciante_id),
    CONSTRAINT fk_denuncia_denunciante
        FOREIGN KEY (denunciante_id)
        REFERENCES denunciantes(id),
    CONSTRAINT fk_denuncia_incidente
        FOREIGN KEY (incidente_id)
        REFERENCES incidentes(id)
);

-- =============================================================================
-- TABLA: asignaciones
-- Vincula un Agente con un Incidente (a través de la Denuncia).
-- Un incidente puede tener múltiples asignaciones históricas,
-- pero solo una ACTIVA en un momento dado.
-- =============================================================================
CREATE TABLE IF NOT EXISTS asignaciones (
    id               VARCHAR(36) NOT NULL,
    fecha_asignacion DATETIME    NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    agente_id        VARCHAR(36) NOT NULL,
    incidente_id     VARCHAR(36) NOT NULL,
    denuncia_id      VARCHAR(36) NULL,
    PRIMARY KEY (id),
    INDEX idx_incidente_estado (incidente_id, estado),
    INDEX idx_agente           (agente_id),
    CONSTRAINT fk_asignacion_agente
        FOREIGN KEY (agente_id)
        REFERENCES agentes(id),
    CONSTRAINT fk_asignacion_incidente
        FOREIGN KEY (incidente_id)
        REFERENCES incidentes(id)
);

-- =============================================================================
-- TABLA: ubicaciones_agente
-- Historial de posiciones GPS del agente durante una atención.
-- Necesaria para el tracking en tiempo real (Fase 2).
-- Se crea ahora para no migrar el schema después.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ubicaciones_agente (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    agente_id     VARCHAR(36)     NOT NULL,
    incidente_id  VARCHAR(36)     NULL,
    latitud       DECIMAL(10,8)   NOT NULL,
    longitud      DECIMAL(11,8)   NOT NULL,
    timestamp     DATETIME        NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    INDEX idx_agente_ts    (agente_id, timestamp),
    INDEX idx_incidente_ts (incidente_id, timestamp)
);
-- =============================================================================
-- TABLA: reportes_hallazgos
-- Generado por el agente al finalizar la atención (paso 9 del flujo).
-- =============================================================================
CREATE TABLE IF NOT EXISTS reportes_hallazgos (
    id            VARCHAR(36)  NOT NULL,
    fecha         DATETIME     NOT NULL,
    descripcion   TEXT         NULL,
    incidente_id  VARCHAR(36)  NOT NULL,
    agente_id     VARCHAR(36)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_incidente (incidente_id),
    CONSTRAINT fk_rh_incidente FOREIGN KEY (incidente_id) REFERENCES incidentes(id),
    CONSTRAINT fk_rh_agente    FOREIGN KEY (agente_id)    REFERENCES agentes(id)
);
 
-- =============================================================================
-- TABLA: reportes_administrativos
-- Generado por el Comando o CAI (pasos 3 y 11 del flujo).
-- =============================================================================
CREATE TABLE IF NOT EXISTS reportes_administrativos (
    id            VARCHAR(36)  NOT NULL,
    fecha         DATETIME     NOT NULL,
    resumen       TEXT         NULL,
    incidente_id  VARCHAR(36)  NOT NULL,
    autoridad_id  VARCHAR(36)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_incidente (incidente_id),
    CONSTRAINT fk_ra_incidente FOREIGN KEY (incidente_id) REFERENCES incidentes(id),
    CONSTRAINT fk_ra_autoridad FOREIGN KEY (autoridad_id) REFERENCES unidades_policiales(id)
);

-- =============================================================================
-- TABLA: auditoria_incidente  (Fase 3)
-- Trazabilidad completa del ciclo de vida del incidente.
-- =============================================================================
CREATE TABLE IF NOT EXISTS auditoria_incidente (
    id                      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    incidente_id            VARCHAR(36)      NOT NULL,
    estado_anterior         VARCHAR(30)      NULL,
    estado_nuevo            VARCHAR(30)      NOT NULL,
    actor_id                VARCHAR(36)      NULL,
    actor_rol               VARCHAR(20)      NULL,
    timestamp               DATETIME         NOT NULL DEFAULT NOW(),
    detalle                 VARCHAR(255)     NULL,
    -- Épica 2: columnas genéricas (ALTER TABLE en vez de tabla nueva —
    -- decisión confirmada con el usuario) para expresar cambios que no son
    -- una transición de estado, ej. "tipo cambió de X a Y". Cuando el
    -- registro es de un cambio de estado normal, estas 3 columnas quedan
    -- NULL; cuando es un cambio de campo genérico, campo/valor_*_generico
    -- se completan y estado_anterior queda NULL (estado_nuevo lleva el
    -- estado vigente al momento del evento, sin representar una transición).
    campo                   VARCHAR(50)      NULL,
    valor_anterior_generico VARCHAR(100)     NULL,
    valor_nuevo_generico    VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    INDEX idx_incidente (incidente_id),
    INDEX idx_timestamp (timestamp)
);
-- =============================================================================
-- TABLA: usuarios
-- Credenciales de autenticación para todos los actores del sistema.
-- actor_id apunta al ID real en denunciantes / agentes / unidades_policiales
-- según el rol, permitiendo que el JWT lleve el ID del entidad de negocio.
-- =============================================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id          VARCHAR(36)  NOT NULL,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,  -- hash BCrypt rounds=10
    rol         VARCHAR(20)  NOT NULL,
    actor_id    VARCHAR(36)  NOT NULL,  -- ID en denunciantes / agentes / unidades_policiales
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    INDEX idx_username (username)
);