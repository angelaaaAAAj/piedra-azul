package com.piedraazul.msagenda.application.ports.in;

import com.piedraazul.msagenda.domain.model.Cita;

public interface CancelarCitaUseCase {
    Cita cancelar(Long citaId);
}