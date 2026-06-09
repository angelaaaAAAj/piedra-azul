package com.piedraazul.msagenda.infrastructure.adapters.out.jpa;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// Interfaz Spring Data JPA — solo la usa el adaptador, nunca el dominio
interface CitaJpaRepository extends JpaRepository<Cita, Long> {
    // ...existing code...
    List<Cita> findByPacienteId(Long pacienteId);
    List<Cita> findByMedicoIdAndEstadoNot(Long medicoId, EstadoCita estado);
    boolean existsByMedicoIdAndFechaHora(Long medicoId, LocalDateTime fechaHora);

    // Fetch JOIN para evitar LazyInitializationException al serializar
    @Query("SELECT DISTINCT c FROM Cita c LEFT JOIN FETCH c.medico WHERE c.medico.id = :medicoId")
    List<Cita> findByMedicoId(@Param("medicoId") Long medicoId);

    // Solo cuenta citas activas (no canceladas ni completadas) en ese horario
    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.medico.id = :medicoId " +
            "AND c.fechaHora = :fechaHora " +
            "AND c.estado NOT IN (com.piedraazul.msagenda.domain.model.EstadoCita.CANCELADA, " +
            "                     com.piedraazul.msagenda.domain.model.EstadoCita.COMPLETADA)")
    boolean existsActiveByMedicoIdAndFechaHora(
            @Param("medicoId") Long medicoId,
            @Param("fechaHora") LocalDateTime fechaHora);
}