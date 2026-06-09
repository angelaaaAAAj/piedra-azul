package com.piedraazul.mshistorial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitaClientService {

    private final RestTemplate restTemplate;
    private static final String MS_AGENDA_URL = "http://localhost:8083";

    public boolean existeCita(Long citaId) {
        try {
            String url = MS_AGENDA_URL + "/api/citas/" + citaId;

            HttpHeaders headers = new HttpHeaders();
            String rol = obtenerRolDelContexto();
            if (rol != null) {
                headers.set("X-User-Role", rol);
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Cita no encontrada en ms-agenda: {}", citaId);
            return false;
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