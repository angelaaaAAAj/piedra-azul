package com.piedraazul.msagenda.service;

import com.piedraazul.msagenda.dto.CitaDTO;
import com.piedraazul.msagenda.event.CitaAgendadaEvent;
import com.piedraazul.msagenda.model.Cita;
import com.piedraazul.msagenda.model.EstadoCita;
import com.piedraazul.msagenda.model.Medico;
import com.piedraazul.msagenda.repository.CitaRepository;
import com.piedraazul.msagenda.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final MedicoRepository medicoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PacienteClientService pacienteClientService;
    private final HistorialClientService historialClientService;

    // Mapa de estrategias disponibles (patrón Strategy)
    private final Map<String, AgendamientoStrategy> estrategias;

    // -- Agendar cita usando Strategy (GoF) --
    public Cita agendar(CitaDTO dto) {

        // Verifica que el paciente existe en ms-pacientes
        if (!pacienteClientService.existePaciente(dto.getPacienteId())) {
            throw new RuntimeException(
                    "Paciente no encontrado en el sistema: " + dto.getPacienteId());
        }

        Medico medico = medicoRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new RuntimeException(
                        "Médico no encontrado: " + dto.getMedicoId()));

        if (!medico.isDisponible()) {
            throw new RuntimeException("El médico no está disponible: "
                    + medico.getNombre());
        }

        // Obtener citas existentes del médico
        List<Cita> citasExistentes = citaRepository
                .findByMedicoIdAndEstadoNot(medico.getId(), EstadoCita.CANCELADA);

        // Determinar fecha y hora
        LocalDateTime fechaHora;
        if (dto.getFechaHoraManual() != null && !dto.getFechaHoraManual().isBlank()) {
            // Paciente eligió horario manual
            fechaHora = LocalDateTime.parse(dto.getFechaHoraManual());
            if (citaRepository.existsByMedicoIdAndFechaHora(medico.getId(), fechaHora)) {
                throw new RuntimeException("El horario ya está ocupado: " + fechaHora);
            }
        } else {
            // Usar Strategy para sugerir horario automáticamente
            String nombreEstrategia = dto.getEstrategia() != null
                    ? dto.getEstrategia() : "primerDisponible";

            AgendamientoStrategy strategy = estrategias.get(nombreEstrategia);
            if (strategy == null) {
                throw new RuntimeException("Estrategia no válida: " + nombreEstrategia);
            }
            fechaHora = strategy.sugerirHorario(medico, citasExistentes);
        }

        // Crear y guardar la cita
        Cita cita = new Cita();
        cita.setPacienteId(dto.getPacienteId());
        cita.setMedico(medico);
        cita.setFechaHora(fechaHora);
        cita.setMotivo(dto.getMotivo());
        cita.setObservaciones(dto.getObservaciones());
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setFechaCreacion(LocalDateTime.now());

        Cita guardada = citaRepository.save(cita);

        // Patrón Observer — notifica a ms-auditoria
        eventPublisher.publishEvent(new CitaAgendadaEvent(
                guardada.getId(),
                guardada.getPacienteId(),
                medico.getId(),
                medico.getNombre() + " " + medico.getApellido(),
                fechaHora,
                dto.getEstrategia(),
                LocalDateTime.now()
        ));

        return guardada;
    }

    // -- Cancelar cita --
    public Cita cancelar(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        cita.setEstado(EstadoCita.CANCELADA);
        return citaRepository.save(cita);
    }

    // -- Reagendar cita --
    public Cita reagendar(Long id, String nuevaFechaHora) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));

        // Guardar fecha anterior ANTES de modificar
        LocalDateTime fechaAnterior = cita.getFechaHora();
        LocalDateTime fechaNueva    = LocalDateTime.parse(nuevaFechaHora);

        if (citaRepository.existsByMedicoIdAndFechaHora(
                cita.getMedico().getId(), fechaNueva)) {
            throw new RuntimeException("El nuevo horario ya está ocupado: " + fechaNueva);
        }

        cita.setFechaHora(fechaNueva);
        cita.setEstado(EstadoCita.REAGENDADA);
        Cita guardada = citaRepository.save(cita);

        // Notificar a ms-historial con fechaAnterior y fechaNueva
        historialClientService.registrarReagendamiento(
                guardada.getId(),
                guardada.getPacienteId(),
                guardada.getMedico().getId(),
                fechaAnterior,
                fechaNueva,
                cita.getMotivo(),
                "sistema"
        );

        return guardada;
    }

    // -- Listar citas por médico --
    public List<Cita> listarPorMedico(Long medicoId) {
        return citaRepository.findByMedicoId(medicoId);
    }

    // -- Listar citas por paciente --
    public List<Cita> listarPorPaciente(Long pacienteId) {
        return citaRepository.findByPacienteId(pacienteId);
    }

    // -- Listar todas --
    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }
    // -- Exportar citas a CSV por médico y fecha --
    public List<Map<String, String>> exportarCitasConDatosPaciente(
            Long medicoId, LocalDate fecha) {

        List<Cita> citas = citaRepository.findByMedicoId(medicoId).stream()
                .filter(c -> fecha == null
                        || c.getFechaHora().toLocalDate().equals(fecha))
                .toList();

        return citas.stream().map(cita -> {
            Map paciente = pacienteClientService.getPaciente(cita.getPacienteId());

            String nombrePaciente = "Desconocido";
            String documento      = "-";

            if (paciente != null) {
                String nombre   = paciente.getOrDefault("nombre",   "").toString();
                String apellido = paciente.getOrDefault("apellido", "").toString();
                nombrePaciente  = (nombre + " " + apellido).trim();
                documento       = paciente.getOrDefault(
                        "numeroDocumento", "-").toString();
            }

            return Map.of(
                    "nombrePaciente", nombrePaciente,
                    "documento",      documento,
                    "hora",           cita.getFechaHora().toLocalTime().toString(),
                    "motivo",         cita.getMotivo()  != null ? cita.getMotivo()  : "",
                    "estado",         cita.getEstado()  != null ? cita.getEstado().name() : ""
            );
        }).toList();
    }

}