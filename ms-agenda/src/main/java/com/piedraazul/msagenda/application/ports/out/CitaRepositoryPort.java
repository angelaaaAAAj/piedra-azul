package com.piedraazul.msagenda.application.ports.out;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Puerto de salida
// El dominio define qué necesita de la BD,
// sin saber que existe JPA ni PostgreSQL.
// ══════════════════════════════════════════════════════
public interface CitaRepositoryPort {
    Cita guardar(Cita cita);
    Optional<Cita> buscarPorId(Long id);
    List<Cita> listarTodas();
    List<Cita> listarPorMedico(Long medicoId);
    List<Cita> listarPorPaciente(Long pacienteId);
    List<Cita> listarPorMedicoExcluyendoEstado(Long medicoId, EstadoCita estado);

    /** Verifica si existe un horario ocupado, ignorando citas CANCELADAS y COMPLETADAS */
    boolean existeHorarioOcupadoActivo(Long medicoId, LocalDateTime fechaHora);

    /** @deprecated Usar existeHorarioOcupadoActivo para no bloquear horarios de citas canceladas */
    @Deprecated
    boolean existeHorarioOcupado(Long medicoId, LocalDateTime fechaHora);
}