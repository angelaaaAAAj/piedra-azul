package com.piedraazul.msagenda.application.ports.out;

import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.domain.model.TipoEspecialidad;

import java.util.List;
import java.util.Optional;

// Puerto de salida para persistencia de médicos
public interface MedicoRepositoryPort {
    Medico guardar(Medico medico);
    Optional<Medico> buscarPorId(Long id);
    List<Medico> listarTodos();
    List<Medico> listarDisponibles();
    List<Medico> listarPorEspecialidadDisponibles(TipoEspecialidad especialidad);
}