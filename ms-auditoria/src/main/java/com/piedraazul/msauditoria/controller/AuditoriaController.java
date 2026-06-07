package com.piedraazul.msauditoria.controller;

import com.piedraazul.msauditoria.model.RegistroAuditoria;
import com.piedraazul.msauditoria.model.TipoEvento;
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
    // Registra un evento manualmente
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
    // Lista todos los registros (HU-11)
    @GetMapping
    public ResponseEntity<List<RegistroAuditoria>> listarTodos() {
        return ResponseEntity.ok(auditoriaService.listarTodos());
    }

    // -- GET /api/auditoria/tipo/{tipoEvento} --
    // Lista por tipo de evento
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
    // Lista acciones de un usuario (HU-12)
    @GetMapping("/usuario/{usuario}")
    public ResponseEntity<List<RegistroAuditoria>> listarPorUsuario(
            @PathVariable String usuario) {
        return ResponseEntity.ok(auditoriaService.listarPorUsuario(usuario));
    }

    // -- GET /api/auditoria/microservicio/{microservicio} --
    // Lista eventos por microservicio origen
    @GetMapping("/microservicio/{microservicio}")
    public ResponseEntity<List<RegistroAuditoria>> listarPorMicroservicio(
            @PathVariable String microservicio) {
        return ResponseEntity.ok(
                auditoriaService.listarPorMicroservicio(microservicio));
    }

    // -- GET /api/auditoria/fechas --
    // Lista eventos en un rango de fechas
    // Ejemplo: /api/auditoria/fechas?inicio=2026-05-01T00:00&fin=2026-05-31T23:59
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