package com.piedraazul.msauth.controller;

import com.piedraazul.msauth.model.Usuario;
import com.piedraazul.msauth.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final RestTemplate restTemplate;

    private static final String MS_PACIENTES_URL = "http://localhost:8082";

    // -- POST /api/auth/registro --
    // Registro de personal (médico, agendador, administrador)
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
    // Registro de paciente nuevo: crea paciente en ms-pacientes
    // y usuario en ms-auth en una sola operación
    @PostMapping("/registro/paciente-nuevo")
    public ResponseEntity<?> registrarPacienteNuevo(
            @RequestBody Map<String, Object> body) {
        try {
            // 1. Crear paciente en ms-pacientes
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

            // 2. Crear usuario en ms-auth con rol PACIENTE
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
    // Registro de paciente que ya existe en el sistema:
    // verifica documento y crea solo el usuario
    @PostMapping("/registro/paciente-existente")
    public ResponseEntity<?> registrarPacienteExistente(
            @RequestBody Map<String, String> body) {
        try {
            String documento = body.get("numeroDocumento");

            // 1. Verificar que el paciente existe en ms-pacientes
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

            // 2. Verificar que no tenga ya un usuario
            if (usuarioService.existePacienteConUsuario(pacienteId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Este paciente ya tiene una cuenta registrada"));
            }

            // 3. Crear usuario en ms-auth con rol PACIENTE
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
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return usuarioService.login(body.get("username"), body.get("password"))
                .map(u -> {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("mensaje", "Login exitoso");
                    response.put("username", u.getUsername());
                    response.put("rol", u.getRol().name());
                    response.put("nombre", u.getNombre());
                    response.put("medicoId", u.getMedicoId());
                    response.put("pacienteId", u.getPacienteId());
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
}