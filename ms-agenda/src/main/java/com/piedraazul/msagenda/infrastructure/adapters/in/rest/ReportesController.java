package com.piedraazul.msagenda.infrastructure.adapters.in.rest;

import com.piedraazul.msagenda.application.ports.out.CitaRepositoryPort;
import com.piedraazul.msagenda.application.ports.out.MedicoRepositoryPort;
import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.security.RolRequerido;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReportesController {

    private final CitaRepositoryPort  citaPort;
    private final MedicoRepositoryPort medicoPort;

    // Citas por mes: { "2026-01": 45, "2026-02": 63, ... }
    @RolRequerido({"ADMINISTRADOR", "MEDICO_TERAPISTA", "AGENDADOR"})
    @GetMapping("/citas-por-mes")
    public ResponseEntity<Map<String, Long>> citasPorMes() {
        Map<String, Long> resultado = citaPort.listarTodas().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getFechaHora().getYear() + "-"
                                + String.format("%02d", c.getFechaHora().getMonthValue()),
                        Collectors.counting()));
        return ResponseEntity.ok(new TreeMap<>(resultado));
    }

    // Citas por médico: [{ "nombre": "Clara Córdoba", "total": 120 }, ...]
    @RolRequerido({"ADMINISTRADOR", "MEDICO_TERAPISTA", "AGENDADOR"})
    @GetMapping("/citas-por-medico")
    public ResponseEntity<List<Map<String, Object>>> citasPorMedico() {
        List<Cita> todas = citaPort.listarTodas();
        List<Medico> medicos = medicoPort.listarTodos();

        List<Map<String, Object>> resultado = medicos.stream().map(m -> {
            long total = todas.stream()
                    .filter(c -> c.getMedico().getId().equals(m.getId()))
                    .count();
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("medicoId",    m.getId());
            fila.put("nombre",      m.getNombre() + " " + m.getApellido());
            fila.put("especialidad", m.getEspecialidad().name());
            fila.put("total",       total);
            return fila;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    // Citas por especialidad: { "QUIROPRAXIA": 200, "FISIOTERAPIA": 150, ... }
    @RolRequerido({"ADMINISTRADOR", "MEDICO_TERAPISTA", "AGENDADOR"})
    @GetMapping("/citas-por-especialidad")
    public ResponseEntity<Map<String, Long>> citasPorEspecialidad() {
        Map<String, Long> resultado = citaPort.listarTodas().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getMedico().getEspecialidad().name(),
                        Collectors.counting()));
        return ResponseEntity.ok(resultado);
    }
}