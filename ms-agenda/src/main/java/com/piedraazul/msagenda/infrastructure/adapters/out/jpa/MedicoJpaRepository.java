package com.piedraazul.msagenda.infrastructure.adapters.out.jpa;

import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.domain.model.TipoEspecialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface MedicoJpaRepository extends JpaRepository<Medico, Long> {
    List<Medico> findByDisponibleTrue();
    List<Medico> findByEspecialidadAndDisponibleTrue(TipoEspecialidad especialidad);
}