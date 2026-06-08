package com.piedraazul.msagenda.application.ports.out;

import java.util.Map;

// Puerto de salida para consultar pacientes en ms-pacientes.
// El dominio no sabe que hay un HTTP call detrás.
public interface PacienteClientPort {
    boolean existePaciente(Long pacienteId);
    Map<String, Object> obtenerPaciente(Long pacienteId);
}