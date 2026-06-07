package com.piedraazul.msauth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad JWT para ms-auth.
 * MISMA clave secreta que en api-gateway (definida en application.properties).
 *
 * Patrón GoF: SINGLETON — Spring crea una sola instancia de este Bean.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private static final long EXPIRACION_MS = 1000L * 60 * 60 * 8; // 8 horas

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String username, String rol) {
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRACION_MS))
                .signWith(secretKey)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esValido(String token) {
        try { validar(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}