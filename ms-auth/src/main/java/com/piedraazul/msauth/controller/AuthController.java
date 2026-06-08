package com.piedraazul.msauth.controller;

import com.piedraazul.msauth.model.Usuario;
import com.piedraazul.msauth.security.JwtTokenProvider;
import com.piedraazul.msauth.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String MS_PACIENTES_URL = "http://localhost:8082";

    // -- POST /api/auth/registro --
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body) {
        try {
            Usuario usuario = usuarioService.crearUsuario(
                    body.get("username"),
                    body.get("password"),
                    body.get("nombre"),
                    body.get("email"),
                    body.get("rol"),
                    null,
                    body.get("medicoId") != null
                            ? Long.parseLong(body.get("medicoId")) : null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- POST /api/auth/registro/paciente-nuevo --
    @PostMapping("/registro/paciente-nuevo")
    public ResponseEntity<?> registrarPacienteNuevo(
            @RequestBody Map<String, Object> body) {
        try {
            String urlPaciente = MS_PACIENTES_URL + "/api/pacientes/registro";
            ResponseEntity<Map> respPaciente = restTemplate.postForEntity(
                    urlPaciente, body, Map.class);

            if (!respPaciente.getStatusCode().is2xxSuccessful()
                    || respPaciente.getBody() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se pudo registrar el paciente"));
            }

            Long pacienteId = Long.parseLong(
                    respPaciente.getBody().get("id").toString());

            Usuario usuario = usuarioService.crearUsuario(
                    body.get("username").toString(),
                    body.get("password").toString(),
                    body.get("nombre").toString(),
                    body.get("email").toString(),
                    "PACIENTE",
                    pacienteId,
                    null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of("mensaje", "Paciente registrado correctamente",
                            "pacienteId", pacienteId,
                            "usuarioId", usuario.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- POST /api/auth/registro/paciente-existente --
    @PostMapping("/registro/paciente-existente")
    public ResponseEntity<?> registrarPacienteExistente(
            @RequestBody Map<String, String> body) {
        try {
            String documento = body.get("numeroDocumento");

            String urlBuscar = MS_PACIENTES_URL
                    + "/api/pacientes/documento/" + documento;
            ResponseEntity<Map> respPaciente = restTemplate.getForEntity(
                    urlBuscar, Map.class);

            if (!respPaciente.getStatusCode().is2xxSuccessful()
                    || respPaciente.getBody() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "No existe un paciente con ese documento"));
            }

            Long pacienteId = Long.parseLong(
                    respPaciente.getBody().get("id").toString());

            if (usuarioService.existePacienteConUsuario(pacienteId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Este paciente ya tiene una cuenta registrada"));
            }

            String nombre = respPaciente.getBody().get("nombre")
                    + " " + respPaciente.getBody().get("apellido");
            Usuario usuario = usuarioService.crearUsuario(
                    body.get("username"),
                    body.get("password"),
                    nombre,
                    body.get("email") != null
                            ? body.get("email")
                            : respPaciente.getBody().get("email").toString(),
                    "PACIENTE",
                    pacienteId,
                    null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of("mensaje", "Cuenta creada correctamente",
                            "pacienteId", pacienteId,
                            "usuarioId", usuario.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- POST /api/auth/login --
    // Ahora genera y retorna un JWT además de los datos del usuario
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return usuarioService.login(body.get("username"), body.get("password"))
                .map(u -> {
                    // Generar el token JWT con los claims del usuario
                    String token = jwtTokenProvider.generarToken(
                            u.getUsername(),
                            u.getRol().name(),
                            u.getPacienteId(),
                            u.getMedicoId()
                    );

                    Map<String, Object> response = new HashMap<>();
                    response.put("token",      token);
                    response.put("mensaje",    "Login exitoso");
                    response.put("username",   u.getUsername());
                    response.put("rol",        u.getRol().name());
                    response.put("nombre",     u.getNombre());
                    response.put("medicoId",   u.getMedicoId());
                    response.put("pacienteId", u.getPacienteId());
                    response.put("usuarioId",  u.getId());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciales incorrectas")));
    }

    // -- GET /api/auth/usuarios --
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // -- GET /api/auth/usuarios/rol/{rol} --
    @GetMapping("/usuarios/rol/{rol}")
    public ResponseEntity<?> listarPorRol(@PathVariable String rol) {
        try {
            return ResponseEntity.ok(usuarioService.listarPorRol(rol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rol no válido: " + rol));
        }
    }

    // -- PATCH /api/auth/usuarios/{id}/desactivar --
    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.desactivarUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // -- PATCH /api/auth/usuarios/{id}/perfil --
// Actualiza nombre, email y/o contraseña del usuario
    @PatchMapping("/usuarios/{id}/perfil")
    public ResponseEntity<?> actualizarPerfil(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        try {
            Usuario usuario = usuarioService.actualizarDatos(
                    id,
                    body.get("nombre"),
                    body.get("email"),
                    body.get("nuevaPassword")
            );
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Perfil actualizado correctamente",
                    "nombre",  usuario.getNombre(),
                    "email",   usuario.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}