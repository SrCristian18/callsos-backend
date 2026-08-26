-- =============================================================================
-- SCRIPT DE INSERCIÓN DE 2 AGENTES Y SUS USUARIOS POR CADA CAI
-- Contraseña plana: password123
-- Hash BCrypt: $2a$10$7R30a3q2sMvt0xL3bJ68/ON4Q2.oZ7Uj3gqWq3c0Y1v81S8l6k1dG
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. REGISTRO EN LA TABLA AGENTES (Agente 1 y Agente 2 para cada CAI)
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 2. REGISTRO EN LA TABLA USUARIOS (Vincula cada agente con su login)
-- -----------------------------------------------------------------------------
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