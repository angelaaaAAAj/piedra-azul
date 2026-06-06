package com.piedraazul.msagenda.controller;

import com.piedraazul.msagenda.dto.CitaDTO;
import com.piedraazul.msagenda.model.Cita;
import com.piedraazul.msagenda.repository.CitaRepository;
import com.piedraazul.msagenda.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final CitaRepository citaRepository;

    // -- POST /api/citas --
    // Agenda una cita (HU-10 agendamiento autónomo)
    @PostMapping
    public ResponseEntity<?> agendar(@Valid @RequestBody CitaDTO dto) {
        try {
            Cita cita = citaService.agendar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(cita);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/citas --
    // Lista todas las citas
    @GetMapping
    public ResponseEntity<List<Cita>> listarTodas() {
        return ResponseEntity.ok(citaService.listarTodas());
    }

    // -- GET /api/citas/medico/{medicoId} --
    // Lista citas por médico (HU-07)
    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<List<Cita>> listarPorMedico(@PathVariable Long medicoId) {
        return ResponseEntity.ok(citaService.listarPorMedico(medicoId));
    }

    // -- GET /api/citas/paciente/{pacienteId} --
    // Lista citas por paciente
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<Cita>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(citaService.listarPorPaciente(pacienteId));
    }

    // -- GET /api/citas/export?medicoId=X&fecha=Y --
    // Exporta citas de un médico en una fecha a CSV (HU exportación)
    // IMPORTANTE: debe ir ANTES de /{id} para que Spring no confunda
    // "export" con un Long y lance NumberFormatException
    @GetMapping("/export")
    public void exportarCsv(
            @RequestParam Long medicoId,
            @RequestParam(required = false) String fecha,
            HttpServletResponse response) {

        try {
            LocalDate localDate = (fecha != null && !fecha.isBlank())
                    ? LocalDate.parse(fecha) : null;

            response.setContentType("text/csv; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"citas_medico_" + medicoId
                            + (localDate != null ? "_" + localDate : "") + ".csv\"");

            PrintWriter writer = response.getWriter();

            // Cabecera del CSV
            writer.println("Nombre Paciente,Documento,Hora,Motivo,Estado");

            // Filas — se convierte el valor a String con valueOf para evitar
            // ClassCastException cuando el Map devuelve Object
            citaService.exportarCitasConDatosPaciente(medicoId, localDate)
                    .forEach(fila -> writer.println(
                            escaparCsv(fila.get("nombrePaciente")) + "," +
                                    escaparCsv(fila.get("documento"))      + "," +
                                    escaparCsv(fila.get("hora"))           + "," +
                                    escaparCsv(fila.get("motivo"))         + "," +
                                    escaparCsv(fila.get("estado"))
                    ));

            writer.flush();

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // -- PATCH /api/citas/{id}/cancelar --
    // Cancela una cita
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(citaService.cancelar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- PATCH /api/citas/{id}/reagendar --
    // Reagenda una cita (HU-04b)
    @PatchMapping("/{id}/reagendar")
    public ResponseEntity<?> reagendar(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                    citaService.reagendar(id, body.get("fechaHora")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/citas/{id} --
    // Busca cita por ID (usado por ms-historial)
    // IMPORTANTE: va DESPUÉS de /export para evitar conflicto de rutas
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return citaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Escapa valores con comas o comillas para CSV válido.
    // Recibe Object (no String) porque el Map<String,Object> del service
    // devuelve Object — así se evita ClassCastException.
    private String escaparCsv(Object valor) {
        if (valor == null) return "";
        String s = String.valueOf(valor);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}