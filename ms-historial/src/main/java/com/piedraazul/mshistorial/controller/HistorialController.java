package com.piedraazul.mshistorial.controller;

import com.piedraazul.mshistorial.dto.HistorialDTO;
import com.piedraazul.mshistorial.model.CambioAgenda;
import com.piedraazul.mshistorial.model.HistorialClinico;
import com.piedraazul.mshistorial.security.RolRequerido;
import com.piedraazul.mshistorial.service.HistorialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class HistorialController {

    private final HistorialService historialService;

    // -- POST /api/historial --
    // Solo médicos/terapistas pueden registrar historia clínica (RNF8)
    @RolRequerido({"MEDICO_TERAPISTA"})
    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody HistorialDTO dto) {
        try {
            HistorialClinico historial = historialService.registrar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(historial);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- POST /api/historial/reagendamiento --
    // Médicos y agendadores pueden registrar reagendamientos
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @PostMapping("/reagendamiento")
    public ResponseEntity<?> registrarReagendamiento(
            @RequestBody Map<String, String> body) {
        try {
            CambioAgenda cambio = historialService.registrarReagendamiento(
                    Long.parseLong(body.get("citaId")),
                    Long.parseLong(body.get("pacienteId")),
                    Long.parseLong(body.get("medicoId")),
                    LocalDateTime.parse(body.get("fechaAnterior")),
                    LocalDateTime.parse(body.get("fechaNueva")),
                    body.get("motivoCambio"),
                    body.get("cambiadoPor")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(cambio);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/historial/paciente/{pacienteId} --
    // Solo roles clínicos y administrativos pueden ver historial (RNF8)
    @RolRequerido({"MEDICO_TERAPISTA", "ADMINISTRADOR"})
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<HistorialClinico>> listarPorPaciente(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(historialService.listarPorPaciente(pacienteId));
    }

    // -- GET /api/historial/cita/{citaId} --
    @RolRequerido({"MEDICO_TERAPISTA", "ADMINISTRADOR"})
    @GetMapping("/cita/{citaId}")
    public ResponseEntity<List<HistorialClinico>> listarPorCita(
            @PathVariable Long citaId) {
        return ResponseEntity.ok(historialService.listarPorCita(citaId));
    }

    // -- GET /api/historial/cambios/cita/{citaId} --
    // Historial de reagendamientos visible para médicos, agendadores y admin
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/cambios/cita/{citaId}")
    public ResponseEntity<List<CambioAgenda>> listarCambiosPorCita(
            @PathVariable Long citaId) {
        return ResponseEntity.ok(historialService.listarCambiosPorCita(citaId));
    }

    // -- GET /api/historial/cambios/paciente/{pacienteId} --
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/cambios/paciente/{pacienteId}")
    public ResponseEntity<List<CambioAgenda>> listarCambiosPorPaciente(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(
                historialService.listarCambiosPorPaciente(pacienteId));
    }
}
