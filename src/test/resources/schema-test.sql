/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  LENOVO
 * Created: 1/06/2026
 */

-- Schema simplificado para H2 en tests de integración.
-- H2 en modo MySQL es compatible con la mayoría de la sintaxis,
-- pero no soporta DECIMAL(10,8) como MySQL ni UUID() como función.

CREATE TABLE IF NOT EXISTS unidades_policiales (
    id          VARCHAR(36)   NOT NULL DEFAULT RANDOM_UUID(),
    nombre      VARCHAR(100)  NOT NULL,
    direccion   VARCHAR(255)  NOT NULL,
    latitud     DOUBLE        NOT NULL,
    longitud    DOUBLE        NOT NULL,
    telefono    VARCHAR(20),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS denunciantes (
    id        VARCHAR(36)  NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    documento VARCHAR(20)  UNIQUE,
    origen    VARCHAR(255),
    telefono  VARCHAR(20),
    correo    VARCHAR(100),
    token_fcm VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS invitaciones_agente (
    token               VARCHAR(64)  NOT NULL,
    unidad_policial_id  VARCHAR(36)  NOT NULL,
    creado_por          VARCHAR(36)  NOT NULL,
    fecha_creacion      TIMESTAMP    NOT NULL,
    fecha_expiracion    TIMESTAMP    NOT NULL,
    usado               BOOLEAN      NOT NULL DEFAULT FALSE,
    usado_por           VARCHAR(36),
    fecha_uso           TIMESTAMP,
    PRIMARY KEY (token)
);

CREATE TABLE IF NOT EXISTS agentes (
    id                 VARCHAR(36)  NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    direccion          VARCHAR(255),
    latitud            DOUBLE,
    longitud           DOUBLE,
    telefono           VARCHAR(20),
    estado             VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    unidad_policial_id VARCHAR(36)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS incidentes (
    id                 VARCHAR(36)  NOT NULL,
    fecha_hora         TIMESTAMP    NOT NULL,
    tipo               VARCHAR(50)  NOT NULL,
    descripcion        CLOB,
    estado             VARCHAR(30)  NOT NULL DEFAULT 'CREADO',
    latitud            DOUBLE       NOT NULL,
    longitud           DOUBLE       NOT NULL,
    denunciante_id     VARCHAR(36)  NOT NULL,
    unidad_policial_id VARCHAR(36),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS asignaciones (
    id               VARCHAR(36) NOT NULL,
    fecha_asignacion TIMESTAMP   NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    agente_id        VARCHAR(36) NOT NULL,
    incidente_id     VARCHAR(36) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ubicaciones_agente (
    id           BIGINT AUTO_INCREMENT,
    agente_id    VARCHAR(36) NOT NULL,
    incidente_id VARCHAR(36),
    latitud      DOUBLE      NOT NULL,
    longitud     DOUBLE      NOT NULL,
    timestamp    TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auditoria_incidente (
    id              BIGINT AUTO_INCREMENT,
    incidente_id    VARCHAR(36)  NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo    VARCHAR(30)  NOT NULL,
    actor_id        VARCHAR(36),
    actor_rol       VARCHAR(20),
    timestamp       TIMESTAMP    NOT NULL DEFAULT NOW(),
    detalle         VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS denuncias (
    id             VARCHAR(36) NOT NULL,
    fecha          TIMESTAMP   NOT NULL,
    tipo           VARCHAR(50) NOT NULL,
    descripcion    CLOB,
    latitud        DOUBLE,
    longitud       DOUBLE,
    denunciante_id VARCHAR(36) NOT NULL,
    incidente_id   VARCHAR(36) NOT NULL,
    PRIMARY KEY (id)
);

-- Agregadas para Épica 4: faltaban en el schema de test aunque ya
-- existían en producción (database/01_schema.sql). Sin estas tablas,
-- ReporteHallazgosRepositoryMySQLTest y ReporteAdministrativoRepositoryMySQLTest
-- fallarían con "Table not found" al arrancar el contexto @JdbcTest.
CREATE TABLE IF NOT EXISTS reportes_hallazgos (
    id           VARCHAR(36) NOT NULL,
    fecha        TIMESTAMP   NOT NULL,
    descripcion  CLOB,
    incidente_id VARCHAR(36) NOT NULL,
    agente_id    VARCHAR(36) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS reportes_administrativos (
    id           VARCHAR(36) NOT NULL,
    fecha        TIMESTAMP   NOT NULL,
    resumen      CLOB,
    incidente_id VARCHAR(36) NOT NULL,
    autoridad_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id        VARCHAR(36)  NOT NULL,
    username  VARCHAR(100) NOT NULL UNIQUE,
    nombre    VARCHAR(150),
    password  VARCHAR(255) NOT NULL,
    rol       VARCHAR(20)  NOT NULL,
    actor_id  VARCHAR(36)  NOT NULL,
    activo    BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);