-- =============================================================================
-- 11_tokens_reseteo_password.sql
-- Épica 8 (hallazgo #6, Parte 2): tokens de un solo uso para recuperación
-- de contraseña. Mismo patrón que invitaciones_agente (04_registro_usuarios.sql),
-- pero sin FK a una tabla de dominio específica — actor_id puede
-- corresponder a denunciantes.id, agentes.id o unidades_policiales.id
-- según de dónde vino el correo (ver SolicitarReseteoPasswordService).
-- =============================================================================

CREATE TABLE IF NOT EXISTS tokens_reseteo_password (
    token             VARCHAR(64)  NOT NULL,
    actor_id          VARCHAR(36)  NOT NULL,
    fecha_creacion    DATETIME     NOT NULL,
    fecha_expiracion  DATETIME     NOT NULL,
    usado             BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_uso         DATETIME     NULL,
    PRIMARY KEY (token),
    INDEX idx_actor (actor_id)
);
