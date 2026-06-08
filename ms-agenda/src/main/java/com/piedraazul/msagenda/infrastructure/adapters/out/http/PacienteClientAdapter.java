package com.piedraazul.msagenda.infrastructure.adapters.out.http;

import com.piedraazul.msagenda.application.ports.out.PacienteClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// ══════════════════════════════════════════════════════
// ARQUITECTURA HEXAGONAL — Adaptador de salida (HTTP)
// Implementa PacienteClientPort. El dominio no sabe
// que hay un RestTemplate ni una URL detrás.
// ══════════════════════════════════════════════════════
@Slf4j
@Component
@RequiredArgsConstructor
public class PacienteClientAdapter implements PacienteClientPort {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "http://localhost:8082/api/pacientes/";

    @Override
    public boolean existePaciente(Long pacienteId) {
        try {
            restTemplate.getForObject(BASE_URL + pacienteId, Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Paciente no encontrado en ms-pacientes: {}", pacienteId);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerPaciente(Long pacienteId) {
        try {
            return restTemplate.getForObject(BASE_URL + pacienteId, Map.class);
        } catch (Exception e) {
            log.error("Error al obtener paciente {}: {}", pacienteId, e.getMessage());
            return null;
        }
    }
}