package com.colorgame.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedIps = new ConcurrentHashMap<>();

    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 3;
    private static final int BLOCK_THRESHOLD = 10;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(LOGIN_LIMIT)
            .refillGreedy(LOGIN_LIMIT, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(REGISTER_LIMIT)
            .refillGreedy(REGISTER_LIMIT, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        String uri = request.getRequestURI();

        // SECURITY: Check if IP is blocked
        Long blockedUntil = blockedIps.get(clientIp);
        if (blockedUntil != null) {
            if (System.currentTimeMillis() < blockedUntil) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"IP bloquée temporairement. Réessayez dans 15 minutes.\"}");
                return;
            } else {
                blockedIps.remove(clientIp);
            }
        }

        if (uri.equals("/api/auth/login")) {
            Bucket bucket = loginBuckets.computeIfAbsent(clientIp, k -> createLoginBucket());
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                filterChain.doFilter(request, response);
            } else {
                // SECURITY: Track failed attempts for IP blocking
                long failedCount = LOGIN_LIMIT - probe.getRemainingTokens();
                if (failedCount >= BLOCK_THRESHOLD) {
                    blockedIps.put(clientIp, System.currentTimeMillis() + BLOCK_DURATION_MS);
                }
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Trop de tentatives de connexion. Réessayez dans une minute.\"}");
            }
        } else if (uri.equals("/api/auth/register")) {
            Bucket bucket = registerBuckets.computeIfAbsent(clientIp, k -> createRegisterBucket());
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Trop de tentatives d'inscription. Réessayez dans une minute.\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
