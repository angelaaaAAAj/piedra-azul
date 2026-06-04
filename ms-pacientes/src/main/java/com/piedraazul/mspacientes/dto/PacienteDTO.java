package com.piedraazul.mspacientes.dto;

import com.piedraazul.mspacientes.model.EstadoPaciente;
import com.piedraazul.mspacientes.model.Genero;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PacienteDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    // obligatoria
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;

    // Opcional según requisito
    @Email(message = "El email no es válido")
    private String email;

    @NotBlank(message = "El celular es obligatorio")
    private String telefono;

    private Genero genero;

    // Campos opcionales
    private String direccion;
    private String eps;
    private EstadoPaciente estado;
}