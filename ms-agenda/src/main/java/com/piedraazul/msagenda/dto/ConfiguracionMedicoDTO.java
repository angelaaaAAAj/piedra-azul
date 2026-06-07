package com.piedraazul.msagenda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para configurar la disponibilidad de un médico.
 * Usado por PATCH /api/medicos/{id}/configuracion
 */
@Data
public class ConfiguracionMedicoDTO {

    // Días de atención: "LUNES,MARTES,MIERCOLES,JUEVES,VIERNES"
    @NotBlank(message = "Los días de atención son obligatorios")
    private String diasAtencion;

    // Formato HH:mm — p. ej. "08:00"
    @NotBlank(message = "La franja de inicio es obligatoria")
    private String franjaInicio;

    // Formato HH:mm — p. ej. "17:00"
    @NotBlank(message = "La franja de fin es obligatoria")
    private String franjaFin;

    // Duración de cada cita en minutos (15, 20, 30, 45, 60)
    @NotNull(message = "El intervalo entre citas es obligatorio")
    @Min(value = 10, message = "El intervalo mínimo es 10 minutos")
    @Max(value = 120, message = "El intervalo máximo es 120 minutos")
    private Integer intervaloCitas;

    // Cuántas semanas hacia adelante puede agendar el paciente (1-12)
    @NotNull(message = "La ventana de semanas es obligatoria")
    @Min(value = 1, message = "Mínimo 1 semana")
    @Max(value = 12, message = "Máximo 12 semanas")
    private Integer ventanaSemanas;
}