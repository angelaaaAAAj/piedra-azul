package com.piedraazul.msagenda.model;

import jakarta.persistence.*;
import lombok.Data;
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

    // Franja horaria de atención (defecto: 08:00 - 17:00)
    @Column(nullable = false)
    private String franjaInicio = "08:00";

    @Column(nullable = false)
    private String franjaFin = "17:00";

    // Duración de cada cita en minutos (defecto: 30)
    @Column(nullable = false)
    private int intervaloCitas = 30;
}