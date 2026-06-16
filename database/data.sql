-- =============================================================================
-- callsos-bd  —  Datos iniciales
-- Versión: Fase 0
-- Se ejecuta después de schema.sql (orden alfabético en initdb.d).
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- Datos de los 26 CAIs reales de Cartagena de Indias
-- Tabla: unidades_policiales  (renombrada desde cais_cartagena)
-- Se agrega columna telefono con NULL donde no se conoce el número.
-- =============================================================================
INSERT INTO unidades_policiales (nombre, direccion, latitud, longitud, telefono) VALUES
('CAI BLAS DE LEZO',          'Barrio Blas de Lezo Mz. 25 Lt 21 4a Etapa, al lado de la Curia',                               10.38765784, -75.48622148, NULL),
('CAI CEBALLOS',              'Barrio ceballos TV 54 CL 22 Diagonal 26L',                                                     10.38842605, -75.50300892, NULL),
('CAI CRESPO',                'Barrio Crespo Kra 56 Nº 31 - 258',                                                             10.44308477, -75.52388451, NULL),
('CAI DANIEL LEMAITRE',       'Barrio Daniel Lemaitre Kra 17 calle 67 Nº 67 - 29 Parque principal',                           10.43767005, -75.52142903, NULL),
('CAI EJECUTIVOS',            'Av. Pedro de Heredia Barrio Chiquinquira Calle 31 Nº 33A -20 Mz 55 Lt 25',                     10.39882948, -75.49315138, NULL),
('CAI EL BOSQUE',             'Avenida el Bosque Transv.52 No 20-109',                                                        10.39271315, -75.52230611, NULL),
('CAI FLOR DEL CAMPO',        'Urbanizacion flor del Campo Manzana 2a Lote 60',                                               10.41837412, -75.44661555, NULL),
('CAI FREDONIA',              'Parque Principal Fredonia Avenida Pedro Romero Calle 32 B Nº 73A - 31 Esquina',                10.40277970, -75.47437169, NULL),
('CAI GAVIOTAS',              'Barrio Las Gaviotas 4A Etapa Mz 43',                                                           10.40177515, -75.48848770, NULL),
('CAI LA ARROCERA',           'Av. Pedro Romero Sector 11 de Noviembre Calle 31 D No. 54-41',                                 10.40816534, -75.49484735, NULL),
('CAI LA CASTELLANA',         'Barrio Las Gaviotas Mz 21 Lt 19 1° Etapa',                                                    10.39795016, -75.48748404, NULL),
('CAI LA ESPERANZA',          'Barrio La Esperanza Kra 32 Nº 39 - 36',                                                        10.41714758, -75.51951913, NULL),
('CAI LA QUINTA',             'Barrio La Quinta calle 34 No. 24 - 85 Calle 1a de las flores',                                 10.41598664, -75.52456989, NULL),
('CAI LAGUITO',               'Laguito Avenida Almirante Brion, Parque Pierino Gallo',                                        10.39622140, -75.56304986, NULL),
('CAI MANGA',                 'Barrio Manga Parte baja Puente Roman, cerca de la Bomba Texaco, Zona verde.',                  10.41772690, -75.54274928, NULL),
('CAI MARIA AUXILIADORA',     'Avenida Pedro de Heredia Kra 38 Barrio Maria Auxiliadora',                                     10.40897980, -75.51572948, NULL),
('CAI NELSON MANDELA',        'CRA. 2 A Nº 78-22 Manzana G',                                                                  10.36747053, -75.48476242, NULL),
('CAI PIEDRA BOLIVAR',        'Barrio Armenia Avenida Comfenalco Carrera 49 calle 30 A',                                      10.40520553, -75.50733508, NULL),
('CAI SAN FRANCISCO',         'Carrera 33 Nº 54I -26 Av. Principal Mz 6',                                                    10.43545047, -75.51554808, NULL),
('CAI SAN JOSÉ DE LOS CAMPANOS', 'Kra 100 No. 39 11',                                                                        10.38702009, -75.45879291, NULL),
('CAI SANTA RITA',            'Kra 17, Calle 53 No. 17-31, al lado de la Alcaldia Menor y mercado Santa Rita',               10.43573739, -75.52801102, NULL),
('CAI SOCORRO',               'Barrio Socorro Plan 134 Mz. 129 Lote 78',                                                      10.38444561, -75.48044879, NULL),
('CAI STELLA MARIS',          'Bocagrande Kra 2A entre la Avenida San Martin y el Pescador, Frente Hospital Naval',           10.41632287, -75.55128968, NULL),
('CAI SAN LAZARO',            'Cl. 29b #18a-246 a 18a-372, Pie de la Popa',                                                  10.41880923, -75.53580536, NULL),
('CAI SAN FERNANDO',          'Cl. 15 #80B-2, Villa Rubia, Cartagena de Indias, Bolívar',                                    10.37983910, -75.47757626, NULL),
('CAI VILLA OLIMPICA',        'Tv. 56 #31-1, Villa Olímpica, Cartagena de Indias, Bolívar',                                  10.40470158, -75.49534321, NULL);

-- =============================================================================
-- Denunciante de prueba
-- Útil para testear el endpoint POST /api/incidentes sin frontend.
-- =============================================================================
INSERT IGNORE INTO denunciantes (id, nombre, origen, telefono, correo, token_fcm) VALUES
('test-denunciante-001', 'Juan Pérez', 'Cartagena', '3001234567', 'juan@test.com', NULL);
-- =============================================================================
-- Usuarios de autenticación — contraseña de todos: "password123"
-- Hash generado con BCrypt rounds=10.
-- Para generar nuevos hashes: https://bcrypt-generator.com
-- o con Java: new BCryptPasswordEncoder().encode("password123")
-- =============================================================================
INSERT IGNORE INTO usuarios (id, username, password, rol, actor_id, activo) VALUES
('usr-001', 'juan.denunciante',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'DENUNCIANTE', 'test-denunciante-001', TRUE),

('usr-002', 'pedro.agente',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'AGENTE', 'test-denunciante-001', TRUE),

('usr-003', 'operador.cai',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'OPERADOR_CAI', 'test-denunciante-001', TRUE),

('usr-004', 'comandante',
 '$2a$10$9aim9M3ypXpg0bN29YA/5.SEBPYqvVXh6ei.6r/Qa156tLtcNCJoe',
 'COMANDO', 'test-denunciante-001', TRUE);