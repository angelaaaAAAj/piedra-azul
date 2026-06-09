package com.piedraazul.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: CHAIN OF RESPONSIBILITY
// Filtro global que intercepta TODAS las peticiones
// antes de que el Gateway las enrute.
//
// Responsabilidades:
//  1. Dejar pasar las rutas públicas sin token
//  2. Extraer y validar el JWT del header Authorization
//  3. Propagar rol, username, pacienteId y medicoId
//     como headers internos hacia los microservicios
//  4. Rechazar con 401 si el token es inválido o falta
// ══════════════════════════════════════════════════════
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Rutas que NO requieren token
    private static final List<String> RUTAS_PUBLICAS = List.of(
            "/api/auth/login",
            "/api/auth/registro/paciente-nuevo",
            "/api/auth/registro/paciente-existente"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Si la ruta es pública, dejar pasar sin validar
        if (esRutaPublica(path)) {
            return chain.filter(exchange);
        }

        // Extraer el header Authorization
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT] Token ausente en ruta: {}", path);
            return rechazar(exchange, "Token de autenticación requerido");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            log.warn("[JWT] Token inválido o expirado en ruta: {}", path);
            return rechazar(exchange, "Token inválido o expirado");
        }

        // Token válido — extraer claims y propagarlos como headers internos
        String username  = jwtUtil.extractUsername(token);
        String rol       = jwtUtil.extractRol(token);
        Long pacienteId  = jwtUtil.extractPacienteId(token);
        Long medicoId    = jwtUtil.extractMedicoId(token);

        log.debug("[JWT] Acceso autorizado | usuario={} | rol={} | ruta={}", username, rol, path);

        // Construir la petición enriquecida con los headers internos
        ServerWebExchange enriquecido = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Username", username)
                        .header("X-User-Role",     rol)
                        .header("X-Paciente-Id",   pacienteId != null ? pacienteId.toString() : "")
                        .header("X-Medico-Id",     medicoId   != null ? medicoId.toString()   : "")
                        .build())
                .build();

        return chain.filter(enriquecido);
    }

    // Orden más alto = se ejecuta primero en la cadena de filtros
    @Override
    public int getOrder() {
        return -1;
    }

    private boolean esRutaPublica(String path) {
        return RUTAS_PUBLICAS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, String mensaje) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var body = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\": \"" + mensaje + "\"}").getBytes());
        return exchange.getResponse().writeWith(Mono.just(body));
    }
}