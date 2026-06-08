package com.piedraazul.msagenda.application.services;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.Medico;

import java.time.LocalDateTime;
import java.util.List;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: STRATEGY
// ══════════════════════════════════════════════════════
public interface AgendamientoStrategy {
    LocalDateTime sugerirHorario(Medico medico, List<Cita> citasExistentes);
}