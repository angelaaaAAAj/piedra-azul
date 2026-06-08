package com.piedraazul.msagenda.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import jakarta.persistence.Version;

@Entity
@Table(name = "citas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pacienteId;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCita estado = EstadoCita.PROGRAMADA;

    @Column
    private String observaciones;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Version
    private Long version;
}