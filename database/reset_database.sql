-- =============================================================================
-- CallS.O.S — reset_database.sql
-- Script maestro consolidado, generado por auditoría técnica.
--
-- QUÉ HACE ESTE SCRIPT
-- --------------------
-- Reconstruye la base de datos completa de CallS.O.S desde cero: crea la
-- base de datos si no existe, elimina y vuelve a crear TODAS las tablas del
-- dominio (en el orden correcto para respetar las foreign keys) y siembra
-- los datos iniciales necesarios para desarrollar y probar la aplicación
-- (26 CAIs reales de Cartagena, usuarios de prueba de los 4 roles, un
-- operador y 2 agentes por CAI).
--
-- ESTRATEGIA ELEGIDA: DROP TABLE IF EXISTS + CREATE TABLE (no
-- "DROP DATABASE"). Por qué:
--   1. El nombre "reset_database.sql" y su propósito (poder ejecutarlo
--      repetidamente para tener una base LIMPIA de desarrollo) piden un
--      reset real y determinista, no un ALTER incremental.
--   2. "DROP DATABASE" exigiría permisos de administrador que el usuario
--      configurado en docker-compose (DB_USER) puede no tener, y además
--      es más agresivo de lo necesario: no necesitamos destruir la base en
--      sí, solo sus tablas.
--   3. DROP TABLE IF EXISTS en orden inverso a las FKs, seguido de
--      CREATE TABLE con el esquema final ya consolidado (fruto de fusionar
--      01_schema.sql + las 5 migraciones ALTER TABLE posteriores), es
--      100% idempotente: se puede correr tantas veces como se quiera y
--      SIEMPRE termina en el mismo estado limpio, sin los problemas de
--      "Duplicate column" que tenían 04_registro_usuarios.sql y
--      05_perfil_usuario.sql al no tener guarda de idempotencia.
--   4. Este script es para entornos de DESARROLLO/PRUEBA (así lo pide la
--      auditoría: "datos necesarios para ejecutar y probar la
--      aplicación"). Para producción, un reset destructivo NO
--      correspondería — ahí sí se usarían migraciones incrementales
--      (Flyway/Liquibase), fuera del alcance de este script.
--
-- Los 9 .sql originales (01 a 11, sin el 03 que ya fue fusionado en el 02
-- según su propio comentario) NO se eliminan de /database — se conservan
-- como historial de migraciones incrementales para entornos que ya tienen
-- datos reales y no pueden pagarse un DROP TABLE. Este script es la
-- alternativa "desde cero" para desarrollo local y CI.
--
-- USO:
--   mysql -u <user> -p <database> < reset_database.sql
--   (o vía docker-entrypoint-initdb.d, donde MySQL solo lo corre la
--   primera vez que el volumen está vacío — ver docker-compose.yml)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS callsos_bd;
USE callsos_bd;

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. DROP (orden inverso a las dependencias FK, seguro con FK_CHECKS=0)
-- =============================================================================
DROP TABLE IF EXISTS tokens_reseteo_password;
DROP TABLE IF EXISTS invitaciones_agente;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS auditoria_incidente;
DROP TABLE IF EXISTS reportes_administrativos;
DROP TABLE IF EXISTS reportes_hallazgos;
DROP TABLE IF EXISTS ubicaciones_agente;
DROP TABLE IF EXISTS asignaciones;
DROP TABLE IF EXISTS denuncias;
DROP TABLE IF EXISTS incidentes;
DROP TABLE IF EXISTS agentes;
DROP TABLE IF EXISTS denunciantes;
DROP TABLE IF EXISTS unidades_policiales;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 2. CREATE TABLE — esquema final consolidado (01 + 04 + 05 + 06 + 07 + 10 + 11)
--    Orden: tablas sin dependencias primero, luego las que las referencian.
-- =============================================================================

-- unidades_policiales (antes cais_cartagena). +telefono +token_fcm (07) +correo (10)
CREATE TABLE unidades_policiales (
    id          VARCHAR(36)   NOT NULL DEFAULT (UUID()),
    nombre      VARCHAR(100)  NOT NULL,
    direccion   VARCHAR(255)  NOT NULL,
    latitud     DECIMAL(10,8) NOT NULL,
    longitud    DECIMAL(11,8) NOT NULL,
    telefono    VARCHAR(20)   NULL,
    correo      VARCHAR(100)  NULL,
    token_fcm   VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    INDEX idx_ubicacion (latitud, longitud)
);

-- denunciantes. +documento (04) +correo ya venía en 01
CREATE TABLE denunciantes (
    id          VARCHAR(36)  NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    documento   VARCHAR(20)  NULL UNIQUE,
    origen      VARCHAR(255) NULL,
    telefono    VARCHAR(20)  NULL,
    correo      VARCHAR(100) NULL,
    token_fcm   VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

-- agentes. +token_fcm (07) +correo (10)
CREATE TABLE agentes (
    id                   VARCHAR(36)  NOT NULL,
    nombre               VARCHAR(100) NOT NULL,
    direccion            VARCHAR(255) NULL,
    latitud              DECIMAL(10,8) NULL,
    longitud             DECIMAL(11,8) NULL,
    telefono             VARCHAR(20)  NULL,
    correo               VARCHAR(100) NULL,
    estado               VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    unidad_policial_id   VARCHAR(36)  NOT NULL,
    token_fcm            VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_estado (estado),
    INDEX idx_unidad (unidad_policial_id),
    CONSTRAINT fk_agente_unidad
        FOREIGN KEY (unidad_policial_id)
        REFERENCES unidades_policiales(id)
);

-- incidentes
CREATE TABLE incidentes (
    id                   VARCHAR(36)   NOT NULL,
    fecha_hora           DATETIME      NOT NULL,
    tipo                 VARCHAR(50)   NOT NULL,
    descripcion          TEXT          NULL,
    estado               VARCHAR(30)   NOT NULL DEFAULT 'CREADO',
    latitud              DECIMAL(10,8) NOT NULL,
    longitud             DECIMAL(11,8) NOT NULL,
    denunciante_id       VARCHAR(36)   NOT NULL,
    unidad_policial_id   VARCHAR(36)   NULL,
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

-- denuncias
CREATE TABLE denuncias (
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

-- asignaciones
CREATE TABLE asignaciones (
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

-- ubicaciones_agente
CREATE TABLE ubicaciones_agente (
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

-- reportes_hallazgos
CREATE TABLE reportes_hallazgos (
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

-- reportes_administrativos
CREATE TABLE reportes_administrativos (
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

-- auditoria_incidente. +campo +valor_anterior_generico +valor_nuevo_generico (06)
CREATE TABLE auditoria_incidente (
    id                      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    incidente_id            VARCHAR(36)      NOT NULL,
    estado_anterior         VARCHAR(30)      NULL,
    estado_nuevo            VARCHAR(30)      NOT NULL,
    actor_id                VARCHAR(36)      NULL,
    actor_rol               VARCHAR(20)      NULL,
    timestamp               DATETIME         NOT NULL DEFAULT NOW(),
    detalle                 VARCHAR(255)     NULL,
    campo                   VARCHAR(50)      NULL,
    valor_anterior_generico VARCHAR(100)     NULL,
    valor_nuevo_generico    VARCHAR(100)     NULL,
    PRIMARY KEY (id),
    INDEX idx_incidente (incidente_id),
    INDEX idx_timestamp (timestamp)
);

-- usuarios. +nombre (05)
CREATE TABLE usuarios (
    id          VARCHAR(36)  NOT NULL,
    username    VARCHAR(100) NOT NULL UNIQUE,
    nombre      VARCHAR(150) NULL,
    password    VARCHAR(255) NOT NULL,
    rol         VARCHAR(20)  NOT NULL,
    actor_id    VARCHAR(36)  NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    INDEX idx_username (username)
);

-- invitaciones_agente (04)
CREATE TABLE invitaciones_agente (
    token               VARCHAR(64)  NOT NULL,
    unidad_policial_id  VARCHAR(36)  NOT NULL,
    creado_por          VARCHAR(36)  NOT NULL,
    fecha_creacion      DATETIME     NOT NULL,
    fecha_expiracion    DATETIME     NOT NULL,
    usado               BOOLEAN      NOT NULL DEFAULT FALSE,
    usado_por           VARCHAR(36)  NULL,
    fecha_uso           DATETIME     NULL,
    PRIMARY KEY (token),
    INDEX idx_unidad (unidad_policial_id),
    CONSTRAINT fk_invitacion_unidad
        FOREIGN KEY (unidad_policial_id)
        REFERENCES unidades_policiales(id)
);

-- tokens_reseteo_password (11)
CREATE TABLE tokens_reseteo_password (
    token             VARCHAR(64)  NOT NULL,
    actor_id          VARCHAR(36)  NOT NULL,
    fecha_creacion    DATETIME     NOT NULL,
    fecha_expiracion  DATETIME     NOT NULL,
    usado             BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_uso         DATETIME     NULL,
    PRIMARY KEY (token),
    INDEX idx_actor (actor_id)
);

-- =============================================================================
-- 3. DATOS INICIALES
-- =============================================================================

-- 26 CAIs reales de Cartagena de Indias (fuente: 02_data.sql)
INSERT INTO unidades_policiales (nombre, direccion, latitud, longitud, telefono) VALUES
('CAI BLAS DE LEZO',          'Barrio Blas de Lezo Mz. 25 Lt 21 4a Etapa, al lado de la Curia',                               10.38765784, -75.48622148, NULL),
('CAI CEBALLOS',              'Barrio ceballos TV 54 CL 22 Diagonal 26L',                                                     10.38842605, -75.50300892, NULL),
('CAI CRESPO',                'Barrio Crespo Kra 56 Nº 31 - 258',                                                             10.44308477, -75.52388451, NULL),
('CAI DANIEL LEMAITRE',       'Barrio Daniel Lemaitre Kra 17 calle 67 Nº 67 - 29 Parque principal',                           10.43767005, -75.52142903, NULL),
('CAI EJECUTIVOS',            'Av. Pedro de Heredia Barrio Chiquinquira Calle 31 Nº 33A -20 Mz 55 Lt 25',                     10.39882948, -75.49315138, NULL),
('CAI EL BOSQUE',             'Avenida el Bosque Transv.52 No 20-109',                                                        10.39271315, -75.52230611, NULL),
('CAI FLOR DEL CAMPO',        'Urbanizacion flor del Campo Manzana 2a Lote 60',                                               10.41837412, -75.44661555, NULL),
('CAI FREDONIA',              'Parque Principal Fredonia Avenida Pedro Romero Calle 32 B Nº 73A - 31 Esquina',                10.40277970, -75.47437169, NULL),
('CAI GAVIOTAS',              'Barrio Las Gaviotas 4A Etapa Mz 43',                                                           10.40177515, -75.48848770, NULL),
('CAI LA ARROCERA',           'Av. Pedro Romero Sector 11 de Noviembre Calle 31 D No. 54-41',                                 10.40816534, -75.49484735, NULL),
('CAI LA CASTELLANA',         'Barrio Las Gaviotas Mz 21 Lt 19 1° Etapa',                                                    10.39795016, -75.48748404, NULL),
('CAI LA ESPERANZA',          'Barrio La Esperanza Kra 32 Nº 39 - 36',                                                        10.41714758, -75.51951913, NULL),
('CAI LA QUINTA',             'Barrio La Quinta calle 34 No. 24 - 85 Calle 1a de las flores',                                 10.41598664, -75.52456989, NULL),
('CAI LAGUITO',               'Laguito Avenida Almirante Brion, Parque Pierino Gallo',                                        10.39622140, -75.56304986, NULL),
('CAI MANGA',                 'Barrio Manga Parte baja Puente Roman, cerca de la Bomba Texaco, Zona verde.',                  10.41772690, -75.54274928, NULL),
('CAI MARIA AUXILIADORA',     'Avenida Pedro de Heredia Kra 38 Barrio Maria Auxiliadora',                                     10.40897980, -75.51572948, NULL),
('CAI NELSON MANDELA',        'CRA. 2 A Nº 78-22 Manzana G',                                                                  10.36747053, -75.48476242, NULL),
('CAI PIEDRA BOLIVAR',        'Barrio Armenia Avenida Comfenalco Carrera 49 calle 30 A',                                      10.40520553, -75.50733508, NULL),
('CAI SAN FRANCISCO',         'Carrera 33 Nº 54I -26 Av. Principal Mz 6',                                                    10.43545047, -75.51554808, NULL),
('CAI SAN JOSÉ DE LOS CAMPANOS', 'Kra 100 No. 39 11',                                                                        10.38702009, -75.45879291, NULL),
('CAI SANTA RITA',            'Kra 17, Calle 53 No. 17-31, al lado de la Alcaldia Menor y mercado Santa Rita',               10.43573739, -75.52801102, NULL),
('CAI SOCORRO',               'Barrio Socorro Plan 134 Mz. 129 Lote 78',                                                      10.38444561, -75.48044879, NULL),
('CAI STELLA MARIS',          'Bocagrande Kra 2A entre la Avenida San Martin y el Pescador, Frente Hospital Naval',           10.41632287, -75.55128968, NULL),
('CAI SAN LAZARO',            'Cl. 29b #18a-246 a 18a-372, Pie de la Popa',                                                  10.41880923, -75.53580536, NULL),
('CAI SAN FERNANDO',          'Cl. 15 #80B-2, Villa Rubia, Cartagena de Indias, Bolívar',                                    10.37983910, -75.47757626, NULL),
('CAI VILLA OLIMPICA',        'Tv. 56 #31-1, Villa Olímpica, Cartagena de Indias, Bolívar',                                  10.40470158, -75.49534321, NULL);

-- Denunciante de prueba
INSERT INTO denunciantes (id, nombre, origen, telefono, correo, token_fcm) VALUES
('test-denunciante-001', 'Juan Pérez', 'Cartagena', '3001234567', 'juan@test.com', NULL);

-- Agente de prueba, vinculado a CAI LA ESPERANZA (resolución determinística por nombre)
INSERT INTO agentes (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
VALUES (
    'test-agente-001',
    'Pedro Agente',
    'CAI La Esperanza, Cartagena',
    10.41714758,
    -75.51951913,
    '3009876543',
    'DISPONIBLE',
    (SELECT id FROM unidades_policiales WHERE nombre = 'CAI LA ESPERANZA')
);

-- Usuarios de autenticación de los 4 roles — contraseña de todos: "password123"
-- Hash BCrypt rounds=10: $2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe
INSERT INTO usuarios (id, username, nombre, password, rol, actor_id, activo) VALUES
('usr-001', 'juan.denunciante', 'Juan Pérez',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'DENUNCIANTE', 'test-denunciante-001', TRUE),

('usr-002', 'pedro.agente', 'Pedro Gómez',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'AGENTE', 'test-agente-001', TRUE),

('usr-003', 'operador.cai', 'Operador CAI',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'OPERADOR_CAI', (SELECT id FROM unidades_policiales WHERE nombre = 'CAI LA ESPERANZA'), TRUE),

('usr-004', 'comandante', 'Comandante López',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'COMANDO', 'test-comando-001', TRUE);

-- Un usuario OPERADOR_CAI por cada CAI restante (fuente: 08_operadores-cai.sql)
-- Username: operador.<nombre-cai-sin-prefijo-ni-espacios>, ej. "operador.blasdelezo"
INSERT INTO usuarios (id, username, password, rol, actor_id, activo)
SELECT
    UUID() AS id,
    CONCAT('operador.', LOWER(REPLACE(REPLACE(UPPER(up.nombre), 'CAI ', ''), ' ', ''))) AS username,
    '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe' AS password,
    'OPERADOR_CAI' AS rol,
    up.id AS actor_id,
    TRUE AS activo
FROM unidades_policiales up
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios u
    WHERE u.username = CONCAT('operador.', LOWER(REPLACE(REPLACE(UPPER(up.nombre), 'CAI ', ''), ' ', '')))
);

-- 2 agentes de prueba por cada CAI, con su usuario de login (fuente: 09_agentes_policia.sql)
INSERT INTO agentes (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
SELECT
    UUID() AS id,
    CONCAT('Agente 1 ', up.nombre) AS nombre,
    up.direccion AS direccion,
    up.latitud AS latitud,
    up.longitud AS longitud,
    up.telefono AS telefono,
    'DISPONIBLE' AS estado,
    up.id AS unidad_policial_id
FROM unidades_policiales up
WHERE NOT EXISTS (
    SELECT 1 FROM agentes a
    WHERE a.unidad_policial_id = up.id
      AND a.nombre = CONCAT('Agente 1 ', up.nombre)
);

INSERT INTO agentes (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
SELECT
    UUID() AS id,
    CONCAT('Agente 2 ', up.nombre) AS nombre,
    up.direccion AS direccion,
    up.latitud AS latitud,
    up.longitud AS longitud,
    up.telefono AS telefono,
    'DISPONIBLE' AS estado,
    up.id AS unidad_policial_id
FROM unidades_policiales up
WHERE NOT EXISTS (
    SELECT 1 FROM agentes a
    WHERE a.unidad_policial_id = up.id
      AND a.nombre = CONCAT('Agente 2 ', up.nombre)
);

INSERT INTO usuarios (id, username, password, rol, actor_id, activo)
SELECT
    UUID() AS id,
    LOWER(REPLACE(REPLACE(REPLACE(a.nombre, 'CAI ', ''), ' ', '.'), '..', '.')) AS username,
    '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe' AS password,
    'AGENTE' AS rol,
    a.id AS actor_id,
    TRUE AS activo
FROM agentes a
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios u
    WHERE u.actor_id = a.id
);
