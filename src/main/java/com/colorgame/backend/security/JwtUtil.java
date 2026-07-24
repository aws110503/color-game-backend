package com.colorgame.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String role) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String username, String role) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("role", role)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public String extractType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            // SECURITY: Reject algorithm 'none' - jjwt does this by default with verifyWith()
            // SECURITY: Reject refresh tokens used as access tokens
            String type = claims.get("type", String.class);
            if ("refresh".equals(type)) {
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            return "refresh".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
