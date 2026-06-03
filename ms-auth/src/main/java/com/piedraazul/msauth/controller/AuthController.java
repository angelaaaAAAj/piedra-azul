package com.piedraazul.msauth.controller;

import com.piedraazul.msauth.model.Usuario;
import com.piedraazul.msauth.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    // -- POST /api/auth/registro --
    // Crea un nuevo usuario (HU-06, HU-07, HU-08, HU-09)
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body) {
        try {
            Usuario usuario = usuarioService.crearUsuario(
                    body.get("username"),
                    body.get("password"),
                    body.get("nombre"),
                    body.get("email"),
                    body.get("rol")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -- POST /api/auth/login --
    // Valida credenciales (HU-01)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return usuarioService.login(body.get("username"), body.get("password"))
                .map(u -> ResponseEntity.ok(Map.of(
                        "mensaje", "Login exitoso",
                        "username", u.getUsername(),
                        "rol", u.getRol().name(),
                        "nombre", u.getNombre()
                )))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciales incorrectas")));
    }

    // -- GET /api/auth/usuarios --
    // Lista todos los usuarios
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // -- GET /api/auth/usuarios/rol/{rol} --
    // Lista usuarios por rol (HU-07 médicos, HU-08 terapistas)
    @GetMapping("/usuarios/rol/{rol}")
    public ResponseEntity<?> listarPorRol(@PathVariable String rol) {
        try {
            return ResponseEntity.ok(usuarioService.listarPorRol(rol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol no válido: " + rol));
        }
    }

    // -- DELETE /api/auth/usuarios/{id}/desactivar --
    // Desactiva un usuario
    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.desactivarUsuario(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}