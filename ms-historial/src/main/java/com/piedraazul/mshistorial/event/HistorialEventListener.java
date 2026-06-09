package com.piedraazul.mshistorial.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
public class HistorialEventListener {

    private final RestTemplate restTemplate;
    private static final String URL = "http://localhost:8085/api/auditoria";

    @EventListener
    public void onHistorialModificado(HistorialModificadoEvent event) {
        try {
            // Mapear el tipoAccion del evento al TipoEvento de ms-auditoria
            String tipoEvento = event.getTipoAccion().equals("CREACION")
                    ? "HISTORIAL_CREADO"
                    : "HISTORIAL_MODIFICADO";

            Map<String, String> body = Map.of(
                    "tipoEvento",          tipoEvento,
                    "descripcion",         "Historial " + event.getTipoAccion().toLowerCase()
                            + " para paciente " + event.getPacienteId(),
                    "entidadId",           String.valueOf(event.getHistorialId()),
                    "realizadoPor",        event.getRealizadoPor() != null
                            ? event.getRealizadoPor() : "sistema",
                    "microservicioOrigen", "ms-historial"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            String rol = obtenerRolDelContexto();
            headers.set("X-User-Role", rol != null ? rol : "MEDICO_TERAPISTA");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(URL, request, Map.class);
            log.info("Evento de historial registrado en ms-auditoria: {}", tipoEvento);

        } catch (Exception e) {
            log.warn("No se pudo notificar a ms-auditoria desde ms-historial: {}", e.getMessage());
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