package com.piedraazul.msagenda.infrastructure.adapters.out.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: OBSERVER
// ══════════════════════════════════════════════════════
@Data
@AllArgsConstructor
public class CitaAgendadaEvent {
    private Long citaId;
    private Long pacienteId;
    private Long medicoId;
    private String nombreMedico;
    private LocalDateTime fechaHora;
    private String estrategiaUsada;
    private LocalDateTime fechaEvento;
}