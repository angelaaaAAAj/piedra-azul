package com.piedraazul.msagenda.infrastructure.adapters.out.http;

import com.piedraazul.msagenda.application.ports.out.AuditoriaClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

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

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            // Propagar el rol que el API Gateway dejó en el request entrante
            String rol = obtenerRolDelContexto();
            if (rol != null) {
                headers.set("X-User-Role", rol);
            }

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(URL, request, Map.class);
        } catch (Exception e) {
            log.warn("No se pudo notificar a ms-auditoria: {}", e.getMessage());
        }
    }

    private String obtenerRolDelContexto() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("X-User-Role");
            }
        } catch (Exception ignored) {}
        return null;
    }
}