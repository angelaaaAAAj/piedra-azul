package com.piedraazul.msagenda.application.services;

import com.piedraazul.msagenda.application.ports.in.AgendarCitaUseCase;
import com.piedraazul.msagenda.application.ports.in.ExportarCitasUseCase;
import com.piedraazul.msagenda.application.ports.in.ReagendarCitaUseCase;
import com.piedraazul.msagenda.application.ports.out.AuditoriaClientPort;
import com.piedraazul.msagenda.application.ports.out.CitaRepositoryPort;
import com.piedraazul.msagenda.application.ports.out.HistorialClientPort;
import com.piedraazul.msagenda.application.ports.out.MedicoRepositoryPort;
import com.piedraazul.msagenda.application.ports.out.PacienteClientPort;
import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;
import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.CitaDTO;
import com.piedraazul.msagenda.infrastructure.adapters.out.event.CitaAgendadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Servicio de aplicación
// Implementa los casos de uso (puertos de entrada) e
// inyecta puertos de salida. No conoce JPA ni HTTP.
// ══════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
public class CitaService implements AgendarCitaUseCase,
        ReagendarCitaUseCase,
        ExportarCitasUseCase {

    // Puertos de salida — el servicio solo conoce las interfaces
    private final CitaRepositoryPort    citaPort;
    private final MedicoRepositoryPort  medicoPort;
    private final PacienteClientPort    pacientePort;
    private final HistorialClientPort   historialPort;
    private final AuditoriaClientPort   auditoriaPort;

    private final ApplicationEventPublisher eventPublisher;

    // Estrategias de agendamiento (patrón Strategy)
    private final Map<String, AgendamientoStrategy> estrategias;

    // ── Puerto de entrada: AgendarCitaUseCase ──────────
    @Override
    public Cita agendar(CitaDTO dto) {

        if (!pacientePort.existePaciente(dto.getPacienteId())) {
            throw new RuntimeException(
                    "Paciente no encontrado en el sistema: " + dto.getPacienteId());
        }

        Medico medico = medicoPort.buscarPorId(dto.getMedicoId())
                .orElseThrow(() -> new RuntimeException(
                        "Médico no encontrado: " + dto.getMedicoId()));

        if (!medico.isDisponible()) {
            throw new RuntimeException(
                    "El médico no está disponible: " + medico.getNombre());
        }

        List<Cita> citasExistentes = citaPort
                .listarPorMedicoExcluyendoEstado(medico.getId(), EstadoCita.CANCELADA);

        LocalDateTime fechaHora;
        if (dto.getFechaHoraManual() != null && !dto.getFechaHoraManual().isBlank()) {
            fechaHora = LocalDateTime.parse(dto.getFechaHoraManual());
            if (citaPort.existeHorarioOcupado(medico.getId(), fechaHora)) {
                throw new RuntimeException("El horario ya está ocupado: " + fechaHora);
            }
        } else {
            String nombreEstrategia = dto.getEstrategia() != null
                    ? dto.getEstrategia() : "primerDisponible";
            AgendamientoStrategy strategy = estrategias.get(nombreEstrategia);
            if (strategy == null) {
                throw new RuntimeException("Estrategia no válida: " + nombreEstrategia);
            }
            fechaHora = strategy.sugerirHorario(medico, citasExistentes);
        }

        Cita cita = new Cita();
        cita.setPacienteId(dto.getPacienteId());
        cita.setMedico(medico);
        cita.setFechaHora(fechaHora);
        cita.setMotivo(dto.getMotivo());
        cita.setObservaciones(dto.getObservaciones());
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setFechaCreacion(LocalDateTime.now());

        Cita guardada = citaPort.guardar(cita);

        // Observer: notifica a ms-auditoria
        eventPublisher.publishEvent(new CitaAgendadaEvent(
                guardada.getId(), guardada.getPacienteId(), medico.getId(),
                medico.getNombre() + " " + medico.getApellido(),
                fechaHora, dto.getEstrategia(), LocalDateTime.now()
        ));

        return guardada;
    }

    // ── Puerto de entrada: ReagendarCitaUseCase ────────
    @Override
    public Cita reagendar(Long citaId, String nuevaFechaHoraStr) {
        Cita cita = citaPort.buscarPorId(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + citaId));

        LocalDateTime fechaAnterior = cita.getFechaHora();
        LocalDateTime fechaNueva    = LocalDateTime.parse(nuevaFechaHoraStr);

        if (citaPort.existeHorarioOcupado(cita.getMedico().getId(), fechaNueva)) {
            throw new RuntimeException("El nuevo horario ya está ocupado: " + fechaNueva);
        }

        cita.setFechaHora(fechaNueva);
        cita.setEstado(EstadoCita.REAGENDADA);
        Cita guardada = citaPort.guardar(cita);

        historialPort.registrarReagendamiento(
                guardada.getId(), guardada.getPacienteId(), guardada.getMedico().getId(),
                fechaAnterior, fechaNueva, cita.getMotivo(), "sistema"
        );

        return guardada;
    }

    // ── Puerto de entrada: ExportarCitasUseCase ────────
    @Override
    public List<Map<String, String>> exportarCitasConDatosPaciente(
            Long medicoId, LocalDate fecha) {

        return citaPort.listarPorMedico(medicoId).stream()
                .filter(c -> fecha == null || c.getFechaHora().toLocalDate().equals(fecha))
                .map(cita -> {
                    Map<String, Object> paciente =
                            pacientePort.obtenerPaciente(cita.getPacienteId());

                    String nombrePaciente = "Desconocido";
                    String documento      = "-";

                    if (paciente != null) {
                        String nombre   = String.valueOf(paciente.getOrDefault("nombre",   ""));
                        String apellido = String.valueOf(paciente.getOrDefault("apellido", ""));
                        nombrePaciente  = (nombre + " " + apellido).trim();
                        documento       = String.valueOf(
                                paciente.getOrDefault("numeroDocumento", "-"));
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

    // ── Métodos de consulta directa (usados por el controller) ─
    public List<Cita> listarTodas()                        { return citaPort.listarTodas(); }
    public List<Cita> listarPorMedico(Long medicoId)       { return citaPort.listarPorMedico(medicoId); }
    public List<Cita> listarPorPaciente(Long pacienteId)   { return citaPort.listarPorPaciente(pacienteId); }
    public Cita cancelar(Long id) {
        Cita cita = citaPort.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        cita.setEstado(EstadoCita.CANCELADA);
        return citaPort.guardar(cita);
    }
}