package com.piedraazul.msauditoria.controller;

import com.piedraazul.msauditoria.model.RegistroAuditoria;
import com.piedraazul.msauditoria.model.TipoEvento;
import com.piedraazul.msauditoria.security.RolRequerido;
import com.piedraazul.msauditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    // -- POST /api/auditoria --
    // Los microservicios internos registran eventos; en producción esto
    // vendría de llamadas internas sin pasar por el gateway, pero para
    // el alcance académico se permite a todos los roles internos.
    @RolRequerido({"ADMINISTRADOR", "MEDICO_TERAPISTA", "AGENDADOR"})
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body) {
        try {
            RegistroAuditoria registro = auditoriaService.registrar(
                    TipoEvento.valueOf(body.get("tipoEvento").toUpperCase()),
                    body.get("descripcion"),
                    body.get("entidadId") != null
                            ? Long.parseLong(body.get("entidadId")) : null,
                    body.get("realizadoPor"),
                    body.get("microservicioOrigen")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(registro);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/auditoria --
    // Solo administradores pueden consultar el log completo (RNF2)
    @RolRequerido({"ADMINISTRADOR"})
    @GetMapping
    public ResponseEntity<List<RegistroAuditoria>> listarTodos() {
        return ResponseEntity.ok(auditoriaService.listarTodos());
    }

    // -- GET /api/auditoria/tipo/{tipoEvento} --
    @RolRequerido({"ADMINISTRADOR"})
    @GetMapping("/tipo/{tipoEvento}")
    public ResponseEntity<?> listarPorTipo(@PathVariable String tipoEvento) {
        try {
            return ResponseEntity.ok(auditoriaService.listarPorTipo(tipoEvento));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de evento no válido: " + tipoEvento));
        }
    }

    // -- GET /api/auditoria/usuario/{usuario} --
    @RolRequerido({"ADMINISTRADOR"})
    @GetMapping("/usuario/{usuario}")
    public ResponseEntity<List<RegistroAuditoria>> listarPorUsuario(
            @PathVariable String usuario) {
        return ResponseEntity.ok(auditoriaService.listarPorUsuario(usuario));
    }

    // -- GET /api/auditoria/microservicio/{microservicio} --
    @RolRequerido({"ADMINISTRADOR"})
    @GetMapping("/microservicio/{microservicio}")
    public ResponseEntity<List<RegistroAuditoria>> listarPorMicroservicio(
            @PathVariable String microservicio) {
        return ResponseEntity.ok(
                auditoriaService.listarPorMicroservicio(microservicio));
    }

    // -- GET /api/auditoria/fechas --
    @RolRequerido({"ADMINISTRADOR"})
    @GetMapping("/fechas")
    public ResponseEntity<?> listarPorFechas(@RequestParam String inicio,
                                             @RequestParam String fin) {
        try {
            return ResponseEntity.ok(auditoriaService.listarPorFechas(inicio, fin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
