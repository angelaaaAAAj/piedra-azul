package com.piedraazul.msagenda.infrastructure.adapters.in.rest;

import com.piedraazul.msagenda.application.ports.in.AgendarCitaUseCase;
import com.piedraazul.msagenda.application.ports.in.ExportarCitasUseCase;
import com.piedraazul.msagenda.application.ports.in.ReagendarCitaUseCase;
import com.piedraazul.msagenda.application.services.CitaService;
import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.CitaDTO;
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
// ARQUITECTURA HEXAGONAL — Adaptador de entrada (REST)
// Solo conoce los puertos de entrada (casos de uso).
// ══════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    // Inyecta los puertos (interfaces), no la implementación
    private final AgendarCitaUseCase    agendarUseCase;
    private final ReagendarCitaUseCase  reagendarUseCase;
    private final ExportarCitasUseCase  exportarUseCase;
    private final CitaService           citaService;   // para consultas directas

    @PostMapping
    public ResponseEntity<?> agendar(@Valid @RequestBody CitaDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(agendarUseCase.agendar(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Cita>> listarTodas() {
        return ResponseEntity.ok(citaService.listarTodas());
    }

    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<List<Cita>> listarPorMedico(@PathVariable Long medicoId) {
        return ResponseEntity.ok(citaService.listarPorMedico(medicoId));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<Cita>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(citaService.listarPorPaciente(pacienteId));
    }

    @GetMapping("/export")
    public void exportarExcel(@RequestParam Long medicoId,
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

                String[] columnas = { "Nombre Paciente", "Documento", "Hora", "Motivo", "Estado" };
                Row filaEncabezado = sheet.createRow(0);
                filaEncabezado.setHeightInPoints(20);
                for (int i = 0; i < columnas.length; i++) {
                    Cell celda = filaEncabezado.createCell(i);
                    celda.setCellValue(columnas[i]);
                    celda.setCellStyle(estiloEncabezado);
                }

                String[] claves = { "nombrePaciente", "documento", "hora", "motivo", "estado" };
                int numFila = 1;
                for (Map<String, String> fila : filas) {
                    Row row = sheet.createRow(numFila++);
                    for (int i = 0; i < claves.length; i++) {
                        Cell celda = row.createCell(i);
                        celda.setCellValue(fila.getOrDefault(claves[i], ""));
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
        } catch (IOException | RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void setBordeFino(CellStyle estilo) {
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(citaService.cancelar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/reagendar")
    public ResponseEntity<?> reagendar(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                    reagendarUseCase.reagendar(id, body.get("fechaHora")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return citaService.listarTodas().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}