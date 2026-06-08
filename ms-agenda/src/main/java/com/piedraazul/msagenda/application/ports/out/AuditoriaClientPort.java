package com.piedraazul.msagenda.application.ports.out;

// Puerto de salida para registrar eventos en ms-auditoria
public interface AuditoriaClientPort {
    void registrarEvento(String tipoEvento, String descripcion,
                         Long entidadId, String realizadoPor);
}