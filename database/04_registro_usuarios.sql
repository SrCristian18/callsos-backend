-- =============================================================================
-- 04_registro_usuarios.sql
-- Soporta el registro de nuevos usuarios (Épica 2, punto 3 de la ruta técnica):
--   - Denunciante: autorregistro abierto, usa "documento" como username.
--   - Agente: registro mediante token de invitación generado por COMANDO,
--     que ata el registro a un CAI específico sin que el agente lo elija.
-- =============================================================================

-- Denunciante necesita un identificador único para poder loguearse — el
-- mockup de RegisterDenuncianteView ya recogía "documento" pero la tabla
-- nunca lo persistía. Se agrega NULLABLE porque los denunciantes semilla
-- de 02_data.sql no lo tienen; los registros nuevos sí lo exigen a nivel
-- de aplicación (ver RegistrarDenuncianteService).
ALTER TABLE denunciantes
    ADD COLUMN documento VARCHAR(20) NULL UNIQUE AFTER nombre;

-- Tokens de invitación para registro de agentes.
-- Generados por COMANDO, atados a un CAI (unidad_policial_id) desde su
-- creación — el agente que lo usa NO elige su propio CAI.
CREATE TABLE IF NOT EXISTS invitaciones_agente (
    token               VARCHAR(64)  NOT NULL,
    unidad_policial_id  VARCHAR(36)  NOT NULL,
    creado_por          VARCHAR(36)  NOT NULL,  -- actorId del COMANDO que lo generó
    fecha_creacion      DATETIME     NOT NULL,
    fecha_expiracion    DATETIME     NOT NULL,
    usado               BOOLEAN      NOT NULL DEFAULT FALSE,
    usado_por           VARCHAR(36)  NULL,      -- id del agente creado con este token
    fecha_uso           DATETIME     NULL,
    PRIMARY KEY (token),
    INDEX idx_unidad (unidad_policial_id),
    CONSTRAINT fk_invitacion_unidad
        FOREIGN KEY (unidad_policial_id)
        REFERENCES unidades_policiales(id)
);
