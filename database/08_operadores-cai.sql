-- =============================================================================
-- SCRIPT DE INSERCIÓN DE OPERADORES CAI ADAPTADO A TU BASE DE DATOS
-- Rol asignado: CAI  (o cambia a 'OPERADOR_CAI' si así lo manejas en tus Enums)
-- Contraseña plana: password123
-- Hash BCrypt: $2a$10$7R30a3q2sMvt0xL3bJ68/ON4Q2.oZ7Uj3gqWq3c0Y1v81S8l6k1dG
-- =============================================================================

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