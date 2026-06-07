package com.piedraazul.msauth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// ══════════════════════════════════════════════════════
// PATRÓN GOF: SINGLETON (garantizado por Spring)
// Genera tokens JWT firmados con HS256 al hacer login.
// El token incluye: username, rol, pacienteId, medicoId.
// ══════════════════════════════════════════════════════
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey   = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(String username, String rol,
                               Long pacienteId, Long medicoId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol",        rol);
        claims.put("pacienteId", pacienteId);
        claims.put("medicoId",   medicoId);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
