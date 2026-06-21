/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  LENOVO
 * Created: 21/06/2026
 */

-- =============================================================================
-- fix_actor_ids.sql
-- ─────────────────────────────────────────────────────────────────────────
-- BUG DE DATOS SEED encontrado en validación end-to-end real (no es un bug
-- de código): en data.sql, los 4 usuarios de prueba (juan.denunciante,
-- pedro.agente, operador.cai, comandante) fueron creados con
-- actor_id = 'test-denunciante-001' para los 4, en vez de cada uno apuntar
-- a su propio actor real. Esto rompe TODOS los endpoints de consulta
-- filtrados por actorId (mis-incidentes, asignados, por-cai) para
-- cualquier rol que no sea DENUNCIANTE.
--
-- Este script:
-- 1. Crea un agente de prueba ('test-agente-001') vinculado al CAI
--    LA ESPERANZA (mismo CAI al que ya se derivó el incidente de prueba
--    en la validación end-to-end, para que el flujo completo encadene).
-- 2. Corrige el actor_id de 'pedro.agente' -> apunta al agente real.
-- 3. Corrige el actor_id de 'operador.cai' -> apunta al CAI real
--    (CAI LA ESPERANZA: 28b11146-5f7f-11f1-a9a5-7c8ae11a1551).
-- 4. Corrige el actor_id de 'comandante' -> identificador simbólico único
--    (Comando no tiene tabla de actor propia en el dominio — ningún
--    endpoint hace JOIN contra su actor_id, solo se usa como subject
--    del JWT).
--
-- Ejecutar con:
--   mysql -u callsos_user -p callsos-bd < fix_actor_ids.sql
-- =============================================================================

SET NAMES utf8mb4;

-- 1) Agente de prueba vinculado a CAI LA ESPERANZA
INSERT IGNORE INTO agentes (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
VALUES (
    'test-agente-001',
    'Pedro Agente',
    'CAI La Esperanza, Cartagena',
    10.41714758,
    -75.51951913,
    '3009876543',
    'DISPONIBLE',
    '28b11146-5f7f-11f1-a9a5-7c8ae11a1551'  -- CAI LA ESPERANZA
);

-- 2) Corregir actor_id de pedro.agente
UPDATE usuarios
SET actor_id = 'test-agente-001'
WHERE username = 'pedro.agente';

-- 3) Corregir actor_id de operador.cai -> CAI LA ESPERANZA
UPDATE usuarios
SET actor_id = '28b11146-5f7f-11f1-a9a5-7c8ae11a1551'
WHERE username = 'operador.cai';

-- 4) Corregir actor_id de comandante -> identificador simbólico
--    (no existe tabla 'comandos'; ningún endpoint hace JOIN con este valor)
UPDATE usuarios
SET actor_id = 'test-comando-001'
WHERE username = 'comandante';

-- Verificación
SELECT id, username, rol, actor_id FROM usuarios;