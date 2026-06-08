package com.piedraazul.msagenda.application.ports.in;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.CitaDTO;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Puerto de entrada
// Define el contrato para agendar una cita.
// El controller solo conoce esta interfaz, nunca CitaService.
// ══════════════════════════════════════════════════════
public interface AgendarCitaUseCase {
    Cita agendar(CitaDTO dto);
}