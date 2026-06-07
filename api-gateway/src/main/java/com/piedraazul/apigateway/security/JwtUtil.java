package com.piedraazul.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// ══════════════════════════════════════════════════════
// Utilidad JWT para el API Gateway.
// Solo valida tokens — la GENERACIÓN ocurre en ms-auth.
// ══════════════════════════════════════════════════════
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Extrae todos los claims del token
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Valida que el token sea correcto y no haya expirado
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Extrae el username (subject) del token
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extrae el rol del token
    public String extractRol(String token) {
        return (String) extractAllClaims(token).get("rol");
    }

    // Extrae el pacienteId del token (puede ser null si no es PACIENTE)
    public Long extractPacienteId(String token) {
        Object val = extractAllClaims(token).get("pacienteId");
        return val != null ? Long.valueOf(val.toString()) : null;
    }

    // Extrae el medicoId del token (puede ser null si no es MEDICO)
    public Long extractMedicoId(String token) {
        Object val = extractAllClaims(token).get("medicoId");
        return val != null ? Long.valueOf(val.toString()) : null;
    }
}