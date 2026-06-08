package com.piedraazul.msagenda.infrastructure.adapters.in.rest;

import com.piedraazul.msagenda.application.ports.out.MedicoRepositoryPort;
import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.domain.model.TipoEspecialidad;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.ConfiguracionMedicoDTO;
import com.piedraazul.msagenda.security.RolRequerido;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Adaptador de entrada REST
// Usa MedicoRepositoryPort (puerto de salida) directamente
// ya que la gestión de médicos no tiene lógica de negocio
// compleja que justifique un caso de uso separado.
// ══════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoRepositoryPort medicoPort;

    // -- POST /api/medicos --
    // Solo administradores crean médicos
    @RolRequerido({"ADMINISTRADOR"})
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Medico medico) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(medicoPort.guardar(medico));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -- GET /api/medicos --
    // Personal interno y pacientes pueden ver la lista para agendar
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping
    public ResponseEntity<List<Medico>> listarTodos() {
        return ResponseEntity.ok(medicoPort.listarTodos());
    }

    // -- GET /api/medicos/disponibles --
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/disponibles")
    public ResponseEntity<List<Medico>> listarDisponibles() {
        return ResponseEntity.ok(medicoPort.listarDisponibles());
    }

    // -- GET /api/medicos/especialidad/{especialidad} --
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/especialidad/{especialidad}")
    public ResponseEntity<?> listarPorEspecialidad(@PathVariable String especialidad) {
        try {
            TipoEspecialidad tipo = TipoEspecialidad.valueOf(especialidad.toUpperCase());
            return ResponseEntity.ok(medicoPort.listarPorEspecialidadDisponibles(tipo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Especialidad no válida: " + especialidad));
        }
    }

    // -- GET /api/medicos/{id} --
    @RolRequerido({"PACIENTE", "MEDICO_TERAPISTA", "AGENDADOR", "ADMINISTRADOR"})
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return medicoPort.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -- PATCH /api/medicos/{id}/disponibilidad --
    // Solo administradores cambian la disponibilidad
    @RolRequerido({"ADMINISTRADOR"})
    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> cambiarDisponibilidad(@PathVariable Long id,
                                                   @RequestBody Map<String, Boolean> body) {
        return medicoPort.buscarPorId(id).map(m -> {
            m.setDisponible(body.get("disponible"));
            return ResponseEntity.ok(medicoPort.guardar(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    // -- PATCH /api/medicos/{id}/configuracion --
    // Solo administradores configuran horarios de médicos
    @RolRequerido({"ADMINISTRADOR"})
    @PatchMapping("/{id}/configuracion")
    public ResponseEntity<?> configurar(@PathVariable Long id,
                                        @Valid @RequestBody ConfiguracionMedicoDTO dto) {
        return medicoPort.buscarPorId(id).map(medico -> {
            medico.setDiasAtencion(dto.getDiasAtencion());
            medico.setFranjaInicio(dto.getFranjaInicio());
            medico.setFranjaFin(dto.getFranjaFin());
            medico.setIntervaloCitas(dto.getIntervaloCitas());
            medico.setVentanaSemanas(dto.getVentanaSemanas());
            return ResponseEntity.ok(medicoPort.guardar(medico));
        }).orElse(ResponseEntity.notFound().build());
    }
}
