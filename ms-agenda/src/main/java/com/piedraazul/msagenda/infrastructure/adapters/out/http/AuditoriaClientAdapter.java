package com.piedraazul.msagenda.infrastructure.adapters.out.http;

import com.piedraazul.msagenda.application.ports.out.AuditoriaClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Adaptador de salida — implementa AuditoriaClientPort
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaClientAdapter implements AuditoriaClientPort {

    private final RestTemplate restTemplate;
    private static final String URL = "http://localhost:8085/api/auditoria";

    @Override
    public void registrarEvento(String tipoEvento, String descripcion,
                                Long entidadId, String realizadoPor) {
        try {
            Map<String, String> body = Map.of(
                    "tipoEvento",          tipoEvento,
                    "descripcion",         descripcion,
                    "entidadId",           String.valueOf(entidadId),
                    "realizadoPor",        realizadoPor,
                    "microservicioOrigen", "ms-agenda"
            );
            restTemplate.postForObject(URL, body, Map.class);
        } catch (Exception e) {
            log.warn("No se pudo notificar a ms-auditoria: {}", e.getMessage());
        }
    }
}