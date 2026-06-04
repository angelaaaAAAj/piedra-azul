package com.piedraazul.msagenda.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

// Cliente HTTP para notificar reagendamientos a ms-historial
@Slf4j
@Service
@RequiredArgsConstructor
public class HistorialClientService {

    private final RestTemplate restTemplate;

    private static final String MS_HISTORIAL_URL =
            "http://localhost:8084/api/historial/reagendamiento";

    public void registrarReagendamiento(Long citaId,
                                        Long pacienteId,
                                        Long medicoId,
                                        LocalDateTime fechaAnterior,
                                        LocalDateTime fechaNueva,
                                        String motivoCambio,
                                        String cambiadoPor) {
        try {
            Map<String, String> body = Map.of(
                    "citaId",         String.valueOf(citaId),
                    "pacienteId",     String.valueOf(pacienteId),
                    "medicoId",       String.valueOf(medicoId),
                    "fechaAnterior",  fechaAnterior.toString(),
                    "fechaNueva",     fechaNueva.toString(),
                    "motivoCambio",   motivoCambio != null ? motivoCambio : "Reagendamiento",
                    "cambiadoPor",    cambiadoPor  != null ? cambiadoPor  : "sistema"
            );
            restTemplate.postForObject(MS_HISTORIAL_URL, body, Map.class);
            log.info("Reagendamiento registrado en ms-historial — citaId: {}", citaId);
        } catch (Exception e) {
            log.warn("No se pudo notificar reagendamiento a ms-historial: {}",
                    e.getMessage());
        }
    }
}