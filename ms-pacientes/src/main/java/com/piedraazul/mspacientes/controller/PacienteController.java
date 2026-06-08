package com.piedraazul.mspacientes.controller;

import com.piedraazul.mspacientes.dto.PacienteDTO;
import com.piedraazul.mspacientes.model.Paciente;
import com.piedraazul.mspacientes.security.RolRequerido;
import com.piedraazul.mspacientes.service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    // -- POST /api/pacientes/registro --
    // Registro autónomo: el paciente lo hace desde la UI sin estar logueado aún,
    // pero este endpoint pasa por el flujo de registro en ms-auth, que no requiere token.
    // Si se llama ya autenticado, cualquier rol puede registrar un paciente.
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody PacienteDTO dto) {
        try {
            Paciente paciente = pacienteService.registrar(dto, "AUTONOMO");
            return ResponseEntity.status(HttpStatus.CREATED).body(paciente);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- POST /api/pacientes/registro/recepcionista --
    // Solo agendadores y administradores registran pacientes manualmente
    @RolRequerido({"AGENDADOR", "ADMINISTRADOR", "MEDICO_TERAPISTA"})
    @PostMapping("/registro/recepcionista")
    public ResponseEntity<?> registrarPorRecepcionista(@Valid @RequestBody PacienteDTO dto) {
        try {
            Paciente paciente = pacienteService.registrar(dto, "RECEPCIONISTA");
            return ResponseEntity.status(HttpStatus.CREATED).body(paciente);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/pacientes --
    // Solo personal interno puede listar todos los pacientes
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping
    public ResponseEntity<List<Paciente>> listarTodos() {
        return ResponseEntity.ok(pacienteService.listarTodos());
    }

    // -- GET /api/pacientes/documento/{documento} --
    // Para buscar un paciente al agendar: agendadores, médicos y admin
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/documento/{documento}")
    public ResponseEntity<?> buscarPorDocumento(@PathVariable String documento) {
        try {
            return ResponseEntity.ok(pacienteService.buscarPorDocumento(documento));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/pacientes/estado/{estado} --
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> listarPorEstado(@PathVariable String estado) {
        try {
            return ResponseEntity.ok(pacienteService.listarPorEstado(estado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Estado no válido: " + estado));
        }
    }

    // -- PUT /api/pacientes/{id} --
    // Administradores y agendadores pueden actualizar datos
    @RolRequerido({"AGENDADOR", "ADMINISTRADOR"})
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody PacienteDTO dto) {
        try {
            return ResponseEntity.ok(pacienteService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- PATCH /api/pacientes/{id}/estado --
    // Solo administradores cambian el estado de un paciente
    @RolRequerido({"ADMINISTRADOR"})
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                    pacienteService.cambiarEstado(id, body.get("estado")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/pacientes/test --
    @GetMapping("/test")
    public String test() {
        return "MS PACIENTES FUNCIONANDO";
    }

    // -- GET /api/pacientes/{id} --
    // Usado internamente por ms-agenda; pacientes pueden ver su propio perfil
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return pacienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
