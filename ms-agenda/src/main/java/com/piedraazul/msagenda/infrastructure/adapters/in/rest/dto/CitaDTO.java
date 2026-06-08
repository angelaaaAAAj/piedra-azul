package com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CitaDTO {

    @NotNull(message = "El paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    private String motivo;
    private String observaciones;
    private String fechaHoraManual;
    private String estrategia = "primerDisponible";
}