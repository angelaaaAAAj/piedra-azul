package com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ConfiguracionMedicoDTO {

    @NotBlank(message = "Los días de atención son obligatorios")
    private String diasAtencion;

    @NotBlank(message = "La franja de inicio es obligatoria")
    private String franjaInicio;

    @NotBlank(message = "La franja de fin es obligatoria")
    private String franjaFin;

    @NotNull(message = "El intervalo entre citas es obligatorio")
    @Min(value = 10, message = "El intervalo mínimo es 10 minutos")
    @Max(value = 120, message = "El intervalo máximo es 120 minutos")
    private Integer intervaloCitas;

    @NotNull(message = "La ventana de semanas es obligatoria")
    @Min(value = 1, message = "Mínimo 1 semana")
    @Max(value = 12, message = "Máximo 12 semanas")
    private Integer ventanaSemanas;
}