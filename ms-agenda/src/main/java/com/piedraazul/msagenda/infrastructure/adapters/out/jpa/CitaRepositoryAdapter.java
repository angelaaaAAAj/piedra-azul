package com.piedraazul.msagenda.infrastructure.adapters.out.jpa;

import com.piedraazul.msagenda.application.ports.out.CitaRepositoryPort;
import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Adaptador de salida (JPA)
// Implementa CitaRepositoryPort usando Spring Data JPA.
// El dominio nunca importa esta clase.
// ══════════════════════════════════════════════════════
@Component
@RequiredArgsConstructor
public class CitaRepositoryAdapter implements CitaRepositoryPort {

    private final CitaJpaRepository jpa;

    @Override public Cita guardar(Cita cita)                    { return jpa.save(cita); }
    @Override public Optional<Cita> buscarPorId(Long id)        { return jpa.findById(id); }
    @Override public List<Cita> listarTodas()                   { return jpa.findAll(); }
    @Override public List<Cita> listarPorMedico(Long medicoId)  { return jpa.findByMedicoId(medicoId); }
    @Override public List<Cita> listarPorPaciente(Long id)      { return jpa.findByPacienteId(id); }

    @Override
    public List<Cita> listarPorMedicoExcluyendoEstado(Long medicoId, EstadoCita estado) {
        return jpa.findByMedicoIdAndEstadoNot(medicoId, estado);
    }

    @Override
    public boolean existeHorarioOcupado(Long medicoId, LocalDateTime fechaHora) {
        return jpa.existsByMedicoIdAndFechaHora(medicoId, fechaHora);
    }
}