-- =============================================================================
-- MIGRACIÓN — Épica 5: notificaciones push a Agente/CAI (token_fcm)
--
-- Para despliegues ya existentes (01_schema.sql ya corrido antes de esta
-- épica). Si la base de datos se crea desde cero, 01_schema.sql ya incluye
-- estas columnas y este script no es necesario.
--
-- Mismo patrón portable que database/06_epica2_auditoria_generica.sql:
-- nada de "ADD COLUMN IF NOT EXISTS" (solo MySQL 8.0.29+) — se valida
-- contra information_schema con un procedimiento temporal, compatible
-- con cualquier MySQL 8.x y seguro de correr más de una vez.
-- =============================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS _epica5_add_columna_si_falta $$
CREATE PROCEDURE _epica5_add_columna_si_falta()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'agentes'
          AND COLUMN_NAME  = 'token_fcm'
    ) THEN
        ALTER TABLE agentes
            ADD COLUMN token_fcm VARCHAR(255) NULL AFTER unidad_policial_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'unidades_policiales'
          AND COLUMN_NAME  = 'token_fcm'
    ) THEN
        ALTER TABLE unidades_policiales
            ADD COLUMN token_fcm VARCHAR(255) NULL AFTER telefono;
    END IF;
END $$

DELIMITER ;

CALL _epica5_add_columna_si_falta();

DROP PROCEDURE _epica5_add_columna_si_falta;
