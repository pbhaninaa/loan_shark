package com.loanshark.api.security;

import com.loanshark.api.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    @Value("${jwt.min-secret-length:64}")
    private int minSecretLength;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is not set. Set jwt.secret or JWT_SECRET (min length: " + minSecretLength + ").");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < minSecretLength) {
            throw new IllegalStateException(
                "JWT secret must be at least " + minSecretLength + " bytes (HS512). Current: " + length + ". Set jwt.secret or JWT_SECRET.");
        }
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("role", user.getRole().name())
            .claim("userId", user.getId())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey())
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        Claims claims = parseClaims(token);
        return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Key signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
