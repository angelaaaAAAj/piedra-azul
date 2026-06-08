package com.piedraazul.msagenda.infrastructure.adapters.out.http;

import com.piedraazul.msagenda.application.ports.out.HistorialClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

// Adaptador de salida — implementa HistorialClientPort
@Slf4j
@Component
@RequiredArgsConstructor
public class HistorialClientAdapter implements HistorialClientPort {

    private final RestTemplate restTemplate;
    private static final String URL = "http://localhost:8084/api/historial/reagendamiento";

    @Override
    public void registrarReagendamiento(Long citaId, Long pacienteId, Long medicoId,
                                        LocalDateTime fechaAnterior, LocalDateTime fechaNueva,
                                        String motivoCambio, String cambiadoPor) {
        try {
            Map<String, String> body = Map.of(
                    "citaId",        String.valueOf(citaId),
                    "pacienteId",    String.valueOf(pacienteId),
                    "medicoId",      String.valueOf(medicoId),
                    "fechaAnterior", fechaAnterior.toString(),
                    "fechaNueva",    fechaNueva.toString(),
                    "motivoCambio",  motivoCambio != null ? motivoCambio : "Reagendamiento",
                    "cambiadoPor",   cambiadoPor  != null ? cambiadoPor  : "sistema"
            );
            restTemplate.postForObject(URL, body, Map.class);
            log.info("Reagendamiento registrado en ms-historial — citaId: {}", citaId);
        } catch (Exception e) {
            log.warn("No se pudo notificar reagendamiento a ms-historial: {}", e.getMessage());
        }
    }
}