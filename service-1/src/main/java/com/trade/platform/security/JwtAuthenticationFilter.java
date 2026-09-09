package com.trade.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Validates the JWT presented to Service 1. The token is issued by the
 * NestJS BFF which is the only authentication authority. We ONLY extract
 * identity — no authorization/business decisions happen here.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/info");

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith) || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "Missing bearer token");
            return;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(header.substring(7))
                    .getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                unauthorized(response, "Not an access token");
                return;
            }
            UserContext.set(UUID.fromString(claims.getSubject()));
            chain.doFilter(request, response);
        } catch (JwtException | ClassCastException | IllegalArgumentException e) {
            unauthorized(response, "Invalid or expired token");
        } finally {
            UserContext.clear();
        }
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"timestamp\":\"" + java.time.Instant.now() + "\",\"status\":401,\"message\":\"" + message + "\"}");
    }
}