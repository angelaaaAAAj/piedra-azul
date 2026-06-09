package com.piedraazul.msagenda.infrastructure.adapters.in.rest;

import com.piedraazul.msagenda.application.ports.in.AgendarCitaUseCase;
import com.piedraazul.msagenda.application.ports.in.ExportarCitasUseCase;
import com.piedraazul.msagenda.application.ports.in.ReagendarCitaUseCase;
import com.piedraazul.msagenda.application.ports.in.CancelarCitaUseCase;
import com.piedraazul.msagenda.application.ports.out.CitaRepositoryPort;
import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.CitaDTO;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.CitaResponseDTO;
import com.piedraazul.msagenda.security.RolRequerido;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Adaptador de entrada REST
// Solo conoce los puertos de entrada (casos de uso).
// No importa CitaService directamente.
// ══════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    // Puertos de entrada — el controller solo conoce interfaces
    private final AgendarCitaUseCase    agendarUseCase;
    private final ReagendarCitaUseCase  reagendarUseCase;
    private final ExportarCitasUseCase  exportarUseCase;
    // Puerto de salida usado directamente para la búsqueda por ID
    private final CancelarCitaUseCase cancelarUseCase;
    private final CitaRepositoryPort  citaPort;

    // -- POST /api/citas --
    // Pacientes agendan de forma autónoma; médicos/agendadores también pueden
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @PostMapping
    public ResponseEntity<?> agendar(@Valid @RequestBody CitaDTO dto) {
        try {
            Cita cita = agendarUseCase.agendar(dto);
            // Convertir a DTO para evitar LazyInitializationException
            CitaResponseDTO response = CitaResponseDTO.fromEntity(cita);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/citas --
    // Solo personal interno puede ver todas las citas
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping
    public ResponseEntity<List<CitaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(
            citaPort.listarTodas().stream()
                .map(CitaResponseDTO::fromEntity)
                .toList()
        );
    }

    // -- GET /api/citas/medico/{medicoId} --
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<List<CitaResponseDTO>> listarPorMedico(@PathVariable Long medicoId) {
        return ResponseEntity.ok(
            citaPort.listarPorMedico(medicoId).stream()
                .map(CitaResponseDTO::fromEntity)
                .toList()
        );
    }

    // -- GET /api/citas/paciente/{pacienteId} --
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<CitaResponseDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(
            citaPort.listarPorPaciente(pacienteId).stream()
                .map(CitaResponseDTO::fromEntity)
                .toList()
        );
    }

    // -- GET /api/citas/export --
    // IMPORTANTE: va antes de /{id} para que Spring no confunda "export" con un Long
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/export")
    public void exportarExcel(
            @RequestParam Long medicoId,
            @RequestParam(required = false) String fecha,
            HttpServletResponse response) {

        try {
            LocalDate localDate = (fecha != null && !fecha.isBlank())
                    ? LocalDate.parse(fecha) : null;

            List<Map<String, String>> filas =
                    exportarUseCase.exportarCitasConDatosPaciente(medicoId, localDate);

            String nombreArchivo = "citas_medico_" + medicoId
                    + (localDate != null ? "_" + localDate : "") + ".xlsx";

            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + nombreArchivo + "\"");

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Citas");

                CellStyle estiloEncabezado = workbook.createCellStyle();
                XSSFColor moradoClaro = new XSSFColor(
                        new byte[]{ (byte) 192, (byte) 132, (byte) 252 }, null);
                ((org.apache.poi.xssf.usermodel.XSSFCellStyle) estiloEncabezado)
                        .setFillForegroundColor(moradoClaro);
                estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                Font fuenteEncabezado = workbook.createFont();
                fuenteEncabezado.setBold(true);
                fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
                fuenteEncabezado.setFontHeightInPoints((short) 11);
                estiloEncabezado.setFont(fuenteEncabezado);
                estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
                setBordeFino(estiloEncabezado);

                CellStyle estiloDatos = workbook.createCellStyle();
                estiloDatos.setFillForegroundColor(IndexedColors.WHITE.getIndex());
                estiloDatos.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                setBordeFino(estiloDatos);

                String[] columnas = {
                        "Nombre Paciente", "Documento", "Hora", "Motivo", "Estado"
                };
                Row filaEncabezado = sheet.createRow(0);
                filaEncabezado.setHeightInPoints(20);
                for (int i = 0; i < columnas.length; i++) {
                    Cell celda = filaEncabezado.createCell(i);
                    celda.setCellValue(columnas[i]);
                    celda.setCellStyle(estiloEncabezado);
                }

                String[] claves = {
                        "nombrePaciente", "documento", "hora", "motivo", "estado"
                };
                int numFila = 1;
                for (Map<String, String> fila : filas) {
                    Row row = sheet.createRow(numFila++);
                    for (int i = 0; i < claves.length; i++) {
                        Cell celda = row.createCell(i);
                        Object valor = fila.get(claves[i]);
                        celda.setCellValue(valor != null ? String.valueOf(valor) : "");
                        celda.setCellStyle(estiloDatos);
                    }
                }

                for (int i = 0; i < columnas.length; i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
                }

                workbook.write(response.getOutputStream());
                response.getOutputStream().flush();
            }

        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void setBordeFino(CellStyle estilo) {
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }

    // -- PATCH /api/citas/{id}/cancelar --
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            Cita cita = cancelarUseCase.cancelar(id);
            CitaResponseDTO response = CitaResponseDTO.fromEntity(cita);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- PATCH /api/citas/{id}/reagendar --
    @RolRequerido({"MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @PatchMapping("/{id}/reagendar")
    public ResponseEntity<?> reagendar(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        try {
            Cita cita = reagendarUseCase.reagendar(id, body.get("fechaHora"));
            CitaResponseDTO response = CitaResponseDTO.fromEntity(cita);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/citas/{id} --
    // IMPORTANTE: va DESPUÉS de /export para evitar conflicto de rutas
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return citaPort.buscarPorId(id)
                .map(c -> ResponseEntity.ok(CitaResponseDTO.fromEntity(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @RolRequerido({"MEDICO_TERAPISTA"})
    @PatchMapping("/{id}/completar")
    public ResponseEntity<?> completar(@PathVariable Long id) {
        try {
            Cita cita = cancelarUseCase.completar(id);
            CitaResponseDTO response = CitaResponseDTO.fromEntity(cita);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
