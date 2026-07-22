package com.colorgame.backend.service;

import com.colorgame.backend.dto.*;
import com.colorgame.backend.model.JwtBlacklist;
import com.colorgame.backend.model.RefreshToken;
import com.colorgame.backend.model.SecurityAuditLog;
import com.colorgame.backend.model.User;
import com.colorgame.backend.repository.JwtBlacklistRepository;
import com.colorgame.backend.repository.RefreshTokenRepository;
import com.colorgame.backend.repository.SecurityAuditLogRepository;
import com.colorgame.backend.repository.UserRepository;
import com.colorgame.backend.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityAuditLogRepository auditLogRepository;
    private final JwtBlacklistRepository jwtBlacklistRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       RefreshTokenRepository refreshTokenRepository, SecurityAuditLogRepository auditLogRepository,
                       JwtBlacklistRepository jwtBlacklistRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.jwtBlacklistRepository = jwtBlacklistRepository;
    }

    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Cet identifiant ou cet email est déjà utilisé.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet identifiant ou cet email est déjà utilisé.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole());

        saveRefreshToken(refreshToken, user.getId());

        logAuditEvent("REGISTER_SUCCESS", user.getId(), getIp(httpRequest),
                "POST /api/auth/register", "New user registered: " + user.getUsername());

        return new AuthResponse(token, refreshToken, user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);

        User user = userRepository.findByUsernameOrEmail(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> {
                    logAuditEvent("LOGIN_FAILURE", null, ip, "POST /api/auth/login",
                            "Failed login attempt for: " + request.getIdentifier());
                    return new RuntimeException("Identifiants invalides.");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logAuditEvent("LOGIN_FAILURE", user.getId(), ip, "POST /api/auth/login",
                    "Wrong password for user: " + user.getUsername());
            throw new RuntimeException("Identifiants invalides.");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole());

        saveRefreshToken(refreshToken, user.getId());

        logAuditEvent("LOGIN_SUCCESS", user.getId(), ip, "POST /api/auth/login",
                "User logged in: " + user.getUsername());

        return new AuthResponse(token, refreshToken, user.getUsername(), user.getRole());
    }

    public AuthResponse refreshTokens(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshTokenValue = request.getRefreshToken();
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new RuntimeException("Refresh token requis.");
        }

        if (!jwtUtil.isRefreshTokenValid(refreshTokenValue)) {
            throw new RuntimeException("Refresh token invalide.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token révoqué ou introuvable."));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expiré.");
        }

        String username = jwtUtil.extractUsername(refreshTokenValue);
        String role = jwtUtil.extractRole(refreshTokenValue);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        // Revoke old refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new pair
        String newToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole());

        saveRefreshToken(newRefreshToken, user.getId());

        return new AuthResponse(newToken, newRefreshToken, user.getUsername(), user.getRole());
    }

    public void logout(String token, HttpServletRequest httpRequest) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            try {
                String jti = jwtUtil.extractJti(jwt);
                if (jti != null) {
                    JwtBlacklist blacklist = new JwtBlacklist();
                    blacklist.setJti(jti);
                    blacklist.setExpiresAt(jwtUtil.extractExpiration(jwt).toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                    jwtBlacklistRepository.save(blacklist);
                }
            } catch (Exception ignored) {
            }
        }
        logAuditEvent("LOGOUT", null, getIp(httpRequest),
                "POST /api/auth/logout", "User logged out");
    }

    private void saveRefreshToken(String token, Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);
    }

    private void logAuditEvent(String eventType, Long userId, String ip, String endpoint, String details) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setEventType(eventType);
        log.setUserId(userId);
        log.setIpAddress(ip);
        log.setEndpoint(endpoint);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String getIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
