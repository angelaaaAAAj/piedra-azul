package com.piedraazul.msagenda.application.ports.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Puerto de entrada para exportar citas a Excel
public interface ExportarCitasUseCase {
    List<Map<String, String>> exportarCitasConDatosPaciente(Long medicoId, LocalDate fecha);
}