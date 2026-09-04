-- =============================================================================
-- 10_email_agente_cai.sql
-- Épica 8 (hallazgo #6, Parte 1): agrega columna de correo a agentes y
-- unidades_policiales, requisito previo a implementar recuperación de
-- contraseña por email para los 3 roles (DENUNCIANTE ya tenía
-- `denunciantes.correo` desde 01_schema.sql, pero nunca se recogía en su
-- formulario de registro — ver RegistrarDenuncianteService, corregido en
-- esta misma épica).
--
-- Ambas columnas NULLABLE a propósito:
--   - agentes/unidades_policiales YA tienen filas sembradas (02_data.sql)
--     sin este dato — no hay forma de backfillar un correo real para
--     ellas sin intervención manual.
--   - unidades_policiales en particular NO tiene ningún flujo de
--     "registro" en la aplicación (los CAI se crean únicamente por seed
--     SQL, jamás por un endpoint) — decisión explícita (Épica 8) de
--     dejar la columna lista sin construir ese flujo todavía. Un futuro
--     "Comando crea un CAI" la usaría.
--   - agentes SÍ tiene flujo de registro (RegistrarAgenteConInvitacionService)
--     y ahí el correo pasa a ser obligatorio para cuentas NUEVAS (ver
--     RegistroAgenteRequest) — pero la columna sigue nullable porque las
--     cuentas YA existentes (seed) no lo tienen retroactivamente.
-- =============================================================================

ALTER TABLE agentes
    ADD COLUMN correo VARCHAR(100) NULL AFTER telefono;

ALTER TABLE unidades_policiales
    ADD COLUMN correo VARCHAR(100) NULL AFTER telefono;
