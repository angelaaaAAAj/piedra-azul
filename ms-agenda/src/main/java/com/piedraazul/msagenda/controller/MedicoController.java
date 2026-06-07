package com.piedraazul.msagenda.controller;

import com.piedraazul.msagenda.model.Medico;
import com.piedraazul.msagenda.model.TipoEspecialidad;
import com.piedraazul.msagenda.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.piedraazul.msagenda.dto.ConfiguracionMedicoDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoRepository medicoRepository;

    // -- POST /api/medicos --
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

    // -- GET /api/medicos --
    // Lista todos los médicos
    @GetMapping
    public ResponseEntity<List<Medico>> listarTodos() {
        return ResponseEntity.ok(medicoRepository.findAll());
    }

    // -- GET /api/medicos/disponibles --
    // Lista médicos disponibles
    @GetMapping("/disponibles")
    public ResponseEntity<List<Medico>> listarDisponibles() {
        return ResponseEntity.ok(medicoRepository.findByDisponibleTrue());
    }

    // -- GET /api/medicos/especialidad/{especialidad} --
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
    // -- GET /api/medicos/{id} --
    // Obtiene un médico por ID (para cargar su configuración actual)
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return medicoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -- PATCH /api/medicos/{id}/disponibilidad --
    // Cambia disponibilidad del médico
    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> cambiarDisponibilidad(@PathVariable Long id,
                                                   @RequestBody Map<String, Boolean> body) {
        return medicoRepository.findById(id).map(m -> {
            m.setDisponible(body.get("disponible"));
            return ResponseEntity.ok(medicoRepository.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }
    // -- PATCH /api/medicos/{id}/configuracion --
    // Actualiza la configuración de disponibilidad de un médico (nueva HU)
    @PatchMapping("/{id}/configuracion")
    public ResponseEntity<?> configurar(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracionMedicoDTO dto) {

        return medicoRepository.findById(id)
                .map(medico -> {
                    medico.setDiasAtencion(dto.getDiasAtencion());
                    medico.setFranjaInicio(dto.getFranjaInicio());
                    medico.setFranjaFin(dto.getFranjaFin());
                    medico.setIntervaloCitas(dto.getIntervaloCitas());
                    medico.setVentanaSemanas(dto.getVentanaSemanas());
                    return ResponseEntity.ok(medicoRepository.save(medico));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}