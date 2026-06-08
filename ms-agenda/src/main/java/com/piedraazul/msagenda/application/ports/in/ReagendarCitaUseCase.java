package com.piedraazul.msagenda.application.ports.in;

import com.piedraazul.msagenda.domain.model.Cita;

// Puerto de entrada para reagendar una cita existente
public interface ReagendarCitaUseCase {
    Cita reagendar(Long citaId, String nuevaFechaHora);
}