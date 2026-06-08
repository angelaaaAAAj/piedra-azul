package com.piedraazul.msagenda.application.ports.out;

import java.time.LocalDateTime;

// Puerto de salida para notificar reagendamientos a ms-historial
public interface HistorialClientPort {
    void registrarReagendamiento(Long citaId, Long pacienteId, Long medicoId,
                                 LocalDateTime fechaAnterior, LocalDateTime fechaNueva,
                                 String motivoCambio, String cambiadoPor);
}