package com.piedraazul.msagenda.domain.model;

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

    @Column(nullable = true, unique = false)
    private String registroMedico = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEspecialidad especialidad;

    @Column(nullable = false)
    private boolean disponible = true;

    @Column(nullable = false)
    private String franjaInicio = "08:00";

    @Column(nullable = false)
    private String franjaFin = "17:00";

    @Column(nullable = false)
    private Integer intervaloCitas = 30;

    @Column
    private String diasAtencion;

    @Column
    private Integer ventanaSemanas = 4;
}