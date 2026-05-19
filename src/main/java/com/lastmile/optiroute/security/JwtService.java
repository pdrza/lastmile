package com.lastmile.optiroute.security;

import com.lastmile.optiroute.domain.entity.Store;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

// Responsável por gerar e validar os tokens JWT
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours}") long expirationHours) {

        // a chave precisa ter pelo menos 32 bytes (256 bits) pro algoritmo HMAC-SHA256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // converte horas pra milissegundos
        this.expirationMs = expirationHours * 3600L * 1000L;
    }

    // gera um token JWT com o id da loja dentro
    public String generateToken(Store store) {
        return Jwts.builder()
                .subject(store.getId().toString())   // id da loja vai no "subject" do token
                .claim("email", store.getEmail())    // email como informação extra
                .issuedAt(new Date())                // quando foi gerado
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // quando expira
                .signWith(key)
                .compact();
    }

    // extrai o id da loja que está dentro do token
    public UUID extractStoreId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    // verifica se o token é válido (não expirou e não foi adulterado)
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // abre o token e retorna o conteúdo dele
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
