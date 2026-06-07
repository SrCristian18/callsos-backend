/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  LENOVO
 * Created: 1/06/2026
 */

-- Datos mínimos para tests de integración.
-- Los IDs son fijos para que los tests sean deterministas.
 
INSERT INTO unidades_policiales (id, nombre, direccion, latitud, longitud, telefono)
VALUES ('cai-test-001', 'CAI Test Manga', 'Calle Test 1', 10.41, -75.54, '6010000');
 
INSERT INTO denunciantes (id, nombre, origen, telefono, correo, token_fcm)
VALUES ('den-test-001', 'Juan Test', 'Cartagena', '3001111111', 'juan@test.com', NULL);
 
INSERT INTO agentes (id, nombre, direccion, latitud, longitud, telefono, estado, unidad_policial_id)
VALUES ('ag-test-001', 'Pedro Test', 'Av. Test', 10.41, -75.54, '3002222222', 'DISPONIBLE', 'cai-test-001');



-- Usuario de prueba para LoginServiceTest de integración
-- Contraseña: "password123" hasheada con BCrypt rounds=10
INSERT INTO usuarios (id, username, password, rol, actor_id, activo)
VALUES (
    'usr-test-001',
    'juan.test',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq',
    'DENUNCIANTE',
    'den-test-001',
    TRUE
);

-- Usuario inactivo para verificar que activo=FALSE lo excluye
INSERT INTO usuarios (id, username, password, rol, actor_id, activo)
VALUES (
    'usr-test-002',
    'inactivo.test',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq',
    'DENUNCIANTE',
    'den-test-001',
    FALSE
);