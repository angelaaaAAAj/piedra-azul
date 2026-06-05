package com.piedraazul.msagenda.controller;

import com.piedraazul.msagenda.model.Medico;
import com.piedraazul.msagenda.model.TipoEspecialidad;
import com.piedraazul.msagenda.repository.MedicoRepository;
import com.piedraazul.msagenda.dto.MedicoConfiguracionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoRepository medicoRepository;

    // ── POST /api/medicos ──
    // Crea un médico o terapista (HU-07, HU-08)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Medico medico) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(medicoRepository.save(medico));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/medicos ──
    // Lista todos los médicos
    @GetMapping
    public ResponseEntity<List<Medico>> listarTodos() {
        return ResponseEntity.ok(medicoRepository.findAll());
    }

    // ── GET /api/medicos/disponibles ──
    // Lista médicos disponibles
    @GetMapping("/disponibles")
    public ResponseEntity<List<Medico>> listarDisponibles() {
        return ResponseEntity.ok(medicoRepository.findByDisponibleTrue());
    }

    // ── GET /api/medicos/especialidad/{especialidad} ──
    // Lista médicos por especialidad
    @GetMapping("/especialidad/{especialidad}")
    public ResponseEntity<?> listarPorEspecialidad(
            @PathVariable String especialidad) {
        try {
            TipoEspecialidad tipo = TipoEspecialidad.valueOf(especialidad.toUpperCase());
            return ResponseEntity.ok(
                    medicoRepository.findByEspecialidadAndDisponibleTrue(tipo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Especialidad no válida: " + especialidad));
        }
    }

    // ── PATCH /api/medicos/{id}/disponibilidad ──
    // Cambia disponibilidad del médico
    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> cambiarDisponibilidad(@PathVariable Long id,
                                                   @RequestBody Map<String, Boolean> body) {
        return medicoRepository.findById(id).map(m -> {
            m.setDisponible(body.get("disponible"));
            return ResponseEntity.ok(medicoRepository.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── PUT /api/medicos/{id}/configuracion ──
    // Configura los parámetros de agenda de un médico (Solo ADMINISTRADOR)
    @PutMapping("/{id}/configuracion")
    public ResponseEntity<?> actualizarConfiguracion(
            @PathVariable Long id,
            @RequestBody MedicoConfiguracionDTO dto,
            @RequestHeader(value = "X-Role", required = false) String role) { // <-- Leemos el Header aquí

        // Validamos el rol manualmente con un IF
        if (role == null || !role.equals("ADMINISTRADOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado. Se requiere el rol ADMINISTRADOR.");
        }

        return medicoRepository.findById(id).map(medico -> {
            try {
                // Validamos que el intervalo de citas sea coherente
                if (dto.getIntervaloCitas() <= 0) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "El intervalo de citas debe ser mayor a 0 minutos."));
                }

                // Validamos que las franjas horarias sean lógicas
                if (dto.getFranjaInicio() != null && dto.getFranjaFin() != null) {
                    if (dto.getFranjaInicio().isAfter(dto.getFranjaFin())) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "La franja de inicio no puede ser posterior a la franja de fin."));
                    }

                    // Asignamos los objetos LocalTime directamente (Se eliminaron los .toString())
                    medico.setFranjaInicio(dto.getFranjaInicio());
                    medico.setFranjaFin(dto.getFranjaFin());
                }

                // Asignamos los demás valores del DTO a la entidad Medico
                medico.setDiasAtencion(dto.getDiasAtencion());
                medico.setIntervaloCitas(dto.getIntervaloCitas());
                medico.setVentanaSemanas(dto.getVentanaSemanas());

                // Guardamos los cambios en la base de datos
                Medico medicoActualizado = medicoRepository.save(medico);
                return ResponseEntity.ok(medicoActualizado);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al guardar la configuración: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Médico con ID " + id + " no encontrado.")));
    }
}