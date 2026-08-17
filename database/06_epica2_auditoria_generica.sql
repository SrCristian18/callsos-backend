-- =============================================================================
-- MIGRACIÓN — Épica 2: Auditoría integral (modelo genérico)
--
-- Para despliegues ya existentes (01_schema.sql ya corrido antes de esta
-- épica). Si la base de datos se crea desde cero, 01_schema.sql ya incluye
-- estas columnas y este script no es necesario.
--
-- Decisión (confirmada con el usuario): ALTER TABLE sobre auditoria_incidente
-- existente, no tabla nueva. No requiere migración de datos históricos —
-- las filas existentes quedan con campo/valor_*_generico en NULL, que es
-- exactamente su significado correcto (fueron cambios de estado, no de
-- campo genérico).
--
-- FIX: la primera versión usaba "ADD COLUMN IF NOT EXISTS", sintaxis que
-- solo soporta MySQL 8.0.29+. Esta versión es compatible con cualquier
-- MySQL 8.x — valida contra information_schema con un procedimiento
-- temporal antes de cada ALTER, y no falla si se corre dos veces.
-- =============================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS _epica2_add_columna_si_falta $$
CREATE PROCEDURE _epica2_add_columna_si_falta()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'auditoria_incidente'
          AND COLUMN_NAME  = 'campo'
    ) THEN
        ALTER TABLE auditoria_incidente
            ADD COLUMN campo VARCHAR(50) NULL AFTER detalle;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'auditoria_incidente'
          AND COLUMN_NAME  = 'valor_anterior_generico'
    ) THEN
        ALTER TABLE auditoria_incidente
            ADD COLUMN valor_anterior_generico VARCHAR(100) NULL AFTER campo;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'auditoria_incidente'
          AND COLUMN_NAME  = 'valor_nuevo_generico'
    ) THEN
        ALTER TABLE auditoria_incidente
            ADD COLUMN valor_nuevo_generico VARCHAR(100) NULL AFTER valor_anterior_generico;
    END IF;
END $$

DELIMITER ;

CALL _epica2_add_columna_si_falta();

DROP PROCEDURE _epica2_add_columna_si_falta;