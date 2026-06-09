package com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto;

import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaResponseDTO {
    private Long id;
    private Long pacienteId;
    private MedicoMinDTO medico;
    private LocalDateTime fechaHora;
    private String motivo;
    private EstadoCita estado;
    private String observaciones;
    private LocalDateTime fechaCreacion;
    private Long version;

    /**
     * Convierte una entidad Cita (con Medico cargado) a DTO
     */
    public static CitaResponseDTO fromEntity(Cita cita) {
        return CitaResponseDTO.builder()
                .id(cita.getId())
                .pacienteId(cita.getPacienteId())
                .medico(cita.getMedico() != null ? MedicoMinDTO.fromEntity(cita.getMedico()) : null)
                .fechaHora(cita.getFechaHora())
                .motivo(cita.getMotivo())
                .estado(cita.getEstado())
                .observaciones(cita.getObservaciones())
                .fechaCreacion(cita.getFechaCreacion())
                .version(cita.getVersion())
                .build();
    }

    /**
     * DTO mínimo para Médico (evita carga innecesaria)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicoMinDTO {
        private Long id;
        private String nombre;
        private String apellido;
        private String especialidad;

        public static MedicoMinDTO fromEntity(com.piedraazul.msagenda.domain.model.Medico medico) {
            return MedicoMinDTO.builder()
                    .id(medico.getId())
                    .nombre(medico.getNombre())
                    .apellido(medico.getApellido())
                    .especialidad(medico.getEspecialidad() != null ? medico.getEspecialidad().name() : null)
                    .build();
        }
    }
}

