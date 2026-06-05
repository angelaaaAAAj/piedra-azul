package com.piedraazul.msagenda.dto;

import java.time.LocalTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicoConfiguracionDTO {

    private String diasAtencion;

    private LocalTime franjaInicio; // <-- Aquí tiene que decir LocalTime

    private LocalTime franjaFin;    // <-- Aquí tiene que decir LocalTime

    private int intervaloCitas;

    private int ventanaSemanas;
}
