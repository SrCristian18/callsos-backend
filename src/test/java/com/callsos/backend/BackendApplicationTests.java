package com.callsos.backend;

import org.junit.jupiter.api.Test;
import com.callsos.backend.infrastructure.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
 
/**
 * Test de arranque del contexto de Spring.
 *
 * @ActiveProfiles("test"): usa application-test.yml con H2 en lugar
 * de MySQL, igual que los tests de integración. Sin esto, @SpringBootTest
 * intenta conectarse al MySQL de Docker que no está corriendo en CI
 * ni en la máquina de desarrollo sin docker-compose up.
 *
 * Este test verifica que todos los beans se registran correctamente
 * y que no hay conflictos de inyección de dependencias.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
