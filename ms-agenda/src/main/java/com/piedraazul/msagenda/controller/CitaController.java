package com.piedraazul.msagenda.controller;

import com.piedraazul.msagenda.dto.CitaDTO;
import com.piedraazul.msagenda.model.Cita;
import com.piedraazul.msagenda.repository.CitaRepository;
import com.piedraazul.msagenda.service.CitaService;
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
    // Exporta citas de un médico en una fecha a Excel (.xlsx) con formato:
    //   - Encabezados con fondo morado claro (#C084FC) y texto blanco en negrita
    //   - Columnas con ancho automático ajustado al contenido
    //   - Filas de datos con fondo blanco y bordes finos
    // IMPORTANTE: va antes de /{id} para que Spring no confunda "export" con un Long
    @GetMapping("/export")
    public void exportarExcel(
            @RequestParam Long medicoId,
            @RequestParam(required = false) String fecha,
            HttpServletResponse response) {

        try {
            LocalDate localDate = (fecha != null && !fecha.isBlank())
                    ? LocalDate.parse(fecha) : null;

            List<Map<String, String>> filas =
                    citaService.exportarCitasConDatosPaciente(medicoId, localDate);

            // Nombre del archivo
            String nombreArchivo = "citas_medico_" + medicoId
                    + (localDate != null ? "_" + localDate : "") + ".xlsx";

            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + nombreArchivo + "\"");

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {

                Sheet sheet = workbook.createSheet("Citas");

                // ── Estilo encabezado: fondo morado claro #C084FC, texto blanco negrita ──
                CellStyle estiloEncabezado = workbook.createCellStyle();
                // Color morado claro: RGB 192, 132, 252
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

                // ── Estilo datos: fondo blanco, borde fino ──
                CellStyle estiloDatos = workbook.createCellStyle();
                estiloDatos.setFillForegroundColor(IndexedColors.WHITE.getIndex());
                estiloDatos.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                setBordeFino(estiloDatos);

                // ── Fila de encabezados ──
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

                // ── Filas de datos ──
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

                // ── Ajustar ancho de columnas al contenido ──
                for (int i = 0; i < columnas.length; i++) {
                    sheet.autoSizeColumn(i);
                    // Agregar un poco de margen extra
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

    // Aplica borde fino en los cuatro lados de un estilo de celda
    private void setBordeFino(CellStyle estilo) {
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
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
}