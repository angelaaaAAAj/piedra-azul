package com.piedraazul.mspacientes.model;

import com.piedraazul.mspacientes.dto.PacienteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PacienteFactory — patrón Factory")
class PacienteFactoryTest {

    private PacienteDTO dto;

    @BeforeEach
    void setUp() {
        dto = new PacienteDTO();
        dto.setNombre("Ana");
        dto.setApellido("Torres");
        dto.setNumeroDocumento("123456");
        dto.setTelefono("3001234567");
        dto.setGenero(Genero.MUJER);
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        dto.setEmail("ana@test.com");
        dto.setDireccion("Calle 1 # 2-3");
        dto.setEps("Sura");
    }

    @Test
    @DisplayName("crearDesdeDTO — mapea todos los campos del DTO al Paciente")
    void crearDesdeDTO_mapeoCompleto() {
        Paciente p = PacienteFactory.crearDesdeDTO(dto);

        assertThat(p.getNombre()).isEqualTo("Ana");
        assertThat(p.getApellido()).isEqualTo("Torres");
        assertThat(p.getNumeroDocumento()).isEqualTo("123456");
        assertThat(p.getTelefono()).isEqualTo("3001234567");
        assertThat(p.getGenero()).isEqualTo(Genero.MUJER);
        assertThat(p.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(p.getEmail()).isEqualTo("ana@test.com");
        assertThat(p.getDireccion()).isEqualTo("Calle 1 # 2-3");
        assertThat(p.getEps()).isEqualTo("Sura");
    }

    @Test
    @DisplayName("crearDesdeDTO — estado por defecto es ACTIVO cuando el DTO no lo trae")
    void crearDesdeDTO_sinEstado_asignaActivo() {
        dto.setEstado(null);

        Paciente p = PacienteFactory.crearDesdeDTO(dto);

        assertThat(p.getEstado()).isEqualTo(EstadoPaciente.ACTIVO);
    }

    @Test
    @DisplayName("crearDesdeDTO — respeta el estado cuando el DTO lo trae explícito")
    void crearDesdeDTO_conEstadoExplicito_loRespeta() {
        dto.setEstado(EstadoPaciente.EN_TRATAMIENTO);

        Paciente p = PacienteFactory.crearDesdeDTO(dto);

        assertThat(p.getEstado()).isEqualTo(EstadoPaciente.EN_TRATAMIENTO);
    }

    @Test
    @DisplayName("crearDesdeDTO — campos opcionales null no rompen la creación")
    void crearDesdeDTO_camposOpcionalesNull_noLanzaExcepcion() {
        dto.setEmail(null);
        dto.setDireccion(null);
        dto.setEps(null);
        dto.setFechaNacimiento(null);

        assertThatCode(() -> PacienteFactory.crearDesdeDTO(dto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("crearDesdeDTO — retorna un objeto Paciente no nulo")
    void crearDesdeDTO_retornaPacienteNoNulo() {
        assertThat(PacienteFactory.crearDesdeDTO(dto)).isNotNull();
    }

    @Test
    @DisplayName("crearDesdeDTO — cada llamada produce una instancia distinta")
    void crearDesdeDTO_llamadasMultiples_producenInstanciasDistintas() {
        Paciente p1 = PacienteFactory.crearDesdeDTO(dto);
        Paciente p2 = PacienteFactory.crearDesdeDTO(dto);

        assertThat(p1).isNotSameAs(p2);
    }
}