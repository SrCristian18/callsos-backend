package com.callsos.backend.application.service;

import com.callsos.backend.domain.enums.RolUsuario;
import com.callsos.backend.domain.model.Denunciante;
import com.callsos.backend.domain.port.in.LoginPort;
import com.callsos.backend.domain.port.in.RegistrarDenunciantePort;
import com.callsos.backend.domain.port.out.DenuncianteRepositoryPort;
import com.callsos.backend.domain.port.out.UsuarioRepositoryPort;
import com.callsos.backend.infrastructure.config.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: autorregistro de DENUNCIANTE.
 *
 * FIX: resuelve el registro de denunciante de la Épica 2. Sin autorización
 * previa — cualquiera puede registrarse. "documento" funciona como
 * username (ver nota de diseño en RegistrarDenunciantePort).
 *
 * @Transactional: escribe en DOS tablas (denunciantes + usuarios). Sin
 * esto, si la segunda escritura falla (ej. username duplicado detectado
 * por una constraint concurrente), quedaría un denunciante huérfano sin
 * usuario asociado — inconsistencia que nadie notaría hasta el primer
 * intento de login fallido. Primera vez que el proyecto usa @Transactional;
 * Spring Boot lo habilita automáticamente al detectar el DataSource, no
 * requiere configuración adicional.
 */
public class RegistrarDenuncianteService implements RegistrarDenunciantePort {

    private final DenuncianteRepositoryPort denuncianteRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegistrarDenuncianteService(DenuncianteRepositoryPort denuncianteRepository,
                                        UsuarioRepositoryPort usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        JwtService jwtService) {
        this.denuncianteRepository = denuncianteRepository;
        this.usuarioRepository     = usuarioRepository;
        this.passwordEncoder       = passwordEncoder;
        this.jwtService            = jwtService;
    }

    @Override
    @Transactional
    public LoginPort.LoginResultado ejecutar(RegistroDenuncianteData datos) {

        if (!datos.password().equals(datos.confirmarPassword())) {
            throw new IllegalStateException("Las contraseñas no coinciden.");
        }

        String documento = datos.documento();
        if (documento == null || documento.isBlank()) {
            throw new IllegalStateException("El documento es obligatorio.");
        }

        if (denuncianteRepository.existePorDocumento(documento)) {
            throw new IllegalStateException(
                "Ya existe un denunciante registrado con ese documento.");
        }
        if (usuarioRepository.existePorUsername(documento)) {
            throw new IllegalStateException(
                "Ya existe una cuenta con ese documento.");
        }

        String nombreCompleto = (datos.nombre() + " " + datos.apellido()).trim();
        String denuncianteId  = UUID.randomUUID().toString();

        Denunciante denunciante = new Denunciante(
            denuncianteId,
            nombreCompleto,
            documento,
            null,               // origen — no se recoge en el registro
            datos.telefono(),
            null,               // correo — no se recoge en este formulario
            null                // tokenFcm — se registra después, al abrir la app
        );
        denuncianteRepository.guardar(denunciante);

        String passwordHash = passwordEncoder.encode(datos.password());
        usuarioRepository.guardar(
            UUID.randomUUID().toString(),
            documento,                       // username = documento
            nombreCompleto,
            passwordHash,
            RolUsuario.DENUNCIANTE.name(),
            denuncianteId
        );

        // Autologueo — mismo JWT que emitiría LoginService, evita que el
        // denunciante tenga que volver a escribir sus credenciales.
        String token = jwtService.generarToken(denuncianteId, RolUsuario.DENUNCIANTE.name());
        return new LoginPort.LoginResultado(
            token, denuncianteId, RolUsuario.DENUNCIANTE.name(), nombreCompleto);
    }
}
