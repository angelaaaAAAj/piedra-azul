package com.piedraazul.msagenda;

import com.piedraazul.msagenda.application.ports.out.CitaRepositoryPort;
import com.piedraazul.msagenda.application.ports.out.MedicoRepositoryPort;
import com.piedraazul.msagenda.application.ports.out.PacienteClientPort;
import com.piedraazul.msagenda.application.ports.out.HistorialClientPort;
import com.piedraazul.msagenda.application.ports.out.AuditoriaClientPort;
import com.piedraazul.msagenda.application.services.AgendamientoStrategy;
import com.piedraazul.msagenda.application.services.CitaService;
import com.piedraazul.msagenda.domain.model.Cita;
import com.piedraazul.msagenda.domain.model.EstadoCita;
import com.piedraazul.msagenda.domain.model.Medico;
import com.piedraazul.msagenda.domain.model.TipoEspecialidad;
import com.piedraazul.msagenda.infrastructure.adapters.in.rest.dto.CitaDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitaServiceTest {

    @Mock private CitaRepositoryPort   citaPort;
    @Mock private MedicoRepositoryPort medicoPort;
    @Mock private PacienteClientPort   pacientePort;
    @Mock private HistorialClientPort  historialPort;
    @Mock private AuditoriaClientPort  auditoriaPort;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Map<String, AgendamientoStrategy> estrategias;

    @InjectMocks
    private CitaService citaService;

    // -- Test 1: Cancelar cita cambia estado correctamente --
    @Test
    void testCancelar_CitaExistente_CambiaEstado() {
        Cita cita = new Cita();
        cita.setId(1L);
        cita.setEstado(EstadoCita.PROGRAMADA);

        when(citaPort.buscarPorId(1L)).thenReturn(Optional.of(cita));
        when(citaPort.guardar(any(Cita.class))).thenReturn(cita);

        Cita resultado = citaService.cancelar(1L);

        assertEquals(EstadoCita.CANCELADA, resultado.getEstado());
        verify(citaPort, times(1)).guardar(cita);
    }

    // -- Test 2: Cancelar cita inexistente lanza excepción --
    @Test
    void testCancelar_CitaInexistente_LanzaExcepcion() {
        when(citaPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> citaService.cancelar(99L));
    }

    // -- Test 3: Médico no disponible lanza excepción --
    @Test
    void testAgendar_MedicoNoDisponible_LanzaExcepcion() {
        Medico medico = new Medico();
        medico.setId(1L);
        medico.setNombre("Carlos");
        medico.setDisponible(false);
        medico.setEspecialidad(TipoEspecialidad.FISIOTERAPIA);

        when(medicoPort.buscarPorId(1L)).thenReturn(Optional.of(medico));
        when(pacientePort.existePaciente(1L)).thenReturn(true);

        CitaDTO dto = new CitaDTO();
        dto.setPacienteId(1L);
        dto.setMedicoId(1L);

        assertThrows(RuntimeException.class, () -> citaService.agendar(dto));
    }

    // -- Test 4: Listar citas por paciente --
    @Test
    void testListarPorPaciente_RetornaListaCorrectamente() {
        Cita cita = new Cita();
        cita.setPacienteId(1L);

        when(citaPort.listarPorPaciente(1L)).thenReturn(List.of(cita));

        List<Cita> resultado = citaService.listarPorPaciente(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getPacienteId());
    }
}