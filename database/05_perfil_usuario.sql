-- =============================================================================
-- 05_perfil_usuario.sql
-- Resuelve el Gap 4 de deuda_backend.md (Opción A recomendada: enriquecer
-- AuthResponse con "nombre" en vez de crear un endpoint de perfil separado).
--
-- Por qué en "usuarios" y no en cada tabla de actor (denunciantes/agentes):
--   COMANDO no tiene tabla propia de dominio (ver 03_fix_actor_ids.sql,
--   punto 4 — su actor_id es solo un identificador simbólico sin JOIN).
--   OPERADOR_CAI apunta al CAI (unidades_policiales), no a una persona.
--   Guardar "nombre" en "usuarios" evita un JOIN condicional por rol contra
--   3 tablas distintas, y le da a COMANDO un lugar real donde existir.
-- =============================================================================

ALTER TABLE usuarios
    ADD COLUMN nombre VARCHAR(150) NULL AFTER username;

-- Backfill de los 4 usuarios semilla (antes solo tenían placeholder en el
-- frontend: "<Rol> · <8 chars del actorId>").
UPDATE usuarios SET nombre = 'Juan Pérez'        WHERE username = 'juan.denunciante';
UPDATE usuarios SET nombre = 'Pedro Gómez'       WHERE username = 'pedro.agente';
UPDATE usuarios SET nombre = 'Operador CAI'      WHERE username = 'operador.cai';
UPDATE usuarios SET nombre = 'Comandante López'  WHERE username = 'comandante';
