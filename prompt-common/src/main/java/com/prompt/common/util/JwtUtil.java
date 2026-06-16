package com.prompt.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String DEFAULT_SECRET = "prompt-market-secret-key-min-length-256-bits-for-hs256";
    private static final long DEFAULT_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private static SecretKey getKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String username, String role, String secret, long expiration) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("username", username, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(secret))
                .compact();
    }

    public static String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parser()
                .verifyWith(getKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    public static Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public static String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public static String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public static boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean validate(String token) {
        try {
            parseToken(token);
            return !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
