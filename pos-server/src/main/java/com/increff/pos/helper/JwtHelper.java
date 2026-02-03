package com.increff.pos.helper;

import com.increff.pos.model.constants.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtHelper {

    private static volatile Key key;
    private static volatile long expiryMinutes = 120;

    private JwtHelper() {}

    // Called once from AuthStaticInitConfig
    public static void init(String secret, long expiryMinutes) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("auth.jwt.secret must be at least 32 characters");
        }
        JwtHelper.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        JwtHelper.expiryMinutes = expiryMinutes;
    }

    public static String createToken(String userId, String email, Role role) {
        ensureInitialized();

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiryMinutes * 60);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "email", email,
                        "role", role.name()
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseClaims(String token) throws JwtException {
        ensureInitialized();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static void ensureInitialized() {
        if (key == null) {
            throw new IllegalStateException("JwtHelper not initialized. Ensure auth.jwt.secret is set and AuthStaticInitConfig is loaded.");
        }
    }
}
