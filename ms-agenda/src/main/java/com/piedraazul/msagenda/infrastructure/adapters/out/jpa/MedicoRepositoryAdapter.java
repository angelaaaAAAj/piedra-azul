package com.piedraazul.msagenda.infrastructure.adapters.out.jpa;

import com.piedraazul.msagenda.application.ports.out.MedicoRepositoryPort;
import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.domain.model.TipoEspecialidad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// Adaptador de salida — implementa MedicoRepositoryPort con JPA
@Component
@RequiredArgsConstructor
public class MedicoRepositoryAdapter implements MedicoRepositoryPort {

    private final MedicoJpaRepository jpa;

    @Override public Medico guardar(Medico medico)                 { return jpa.save(medico); }
    @Override public Optional<Medico> buscarPorId(Long id)         { return jpa.findById(id); }
    @Override public List<Medico> listarTodos()                    { return jpa.findAll(); }
    @Override public List<Medico> listarDisponibles()              { return jpa.findByDisponibleTrue(); }

    @Override
    public List<Medico> listarPorEspecialidadDisponibles(TipoEspecialidad especialidad) {
        return jpa.findByEspecialidadAndDisponibleTrue(especialidad);
    }
}