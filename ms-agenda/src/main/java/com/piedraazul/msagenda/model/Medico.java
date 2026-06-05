package com.piedraazul.msagenda.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "medicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String registroMedico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEspecialidad especialidad;

    @Column(nullable = false)
    private boolean disponible = true;

    // --- CAMPOS DE CONFIGURACIÓN ---
    @Column(name = "dias_atencion")
    private String diasAtencion;

    @Column(nullable = false)
    private LocalTime franjaInicio = LocalTime.of(8, 0); // Ahora es LocalTime

    @Column(nullable = false)
    private LocalTime franjaFin = LocalTime.of(17, 0);    // Ahora es LocalTime

    @Column(nullable = false)
    private int intervaloCitas = 30;

    @Column(name = "ventana_semanas", nullable = false)
    private int ventanaSemanas = 4;
}