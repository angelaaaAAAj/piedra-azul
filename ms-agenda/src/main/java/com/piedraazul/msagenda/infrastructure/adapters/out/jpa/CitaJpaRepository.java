package com.piedraazul.msagenda.infrastructure.adapters.out.jpa;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Interfaz Spring Data JPA — solo la usa el adaptador, nunca el dominio
interface CitaJpaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByMedicoId(Long medicoId);
    List<Cita> findByPacienteId(Long pacienteId);
    List<Cita> findByMedicoIdAndEstadoNot(Long medicoId, EstadoCita estado);
    boolean existsByMedicoIdAndFechaHora(Long medicoId, LocalDateTime fechaHora);
}