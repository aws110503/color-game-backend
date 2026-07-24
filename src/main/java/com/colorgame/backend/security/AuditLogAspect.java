package com.colorgame.backend.security;

import com.colorgame.backend.model.SecurityAuditLog;
import com.colorgame.backend.repository.SecurityAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditLogAspect {

    private final SecurityAuditLogRepository auditLogRepository;

    public AuditLogAspect(SecurityAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("@annotation(auditEvent)")
    public Object logAuditEvent(ProceedingJoinPoint joinPoint, AuditEvent auditEvent) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        String endpoint = request != null ? request.getMethod() + " " + request.getRequestURI() : "unknown";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = "anonymous";
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = auth.getName();
        }

        try {
            Object result = joinPoint.proceed();
            logEvent(auditEvent.value(), userId, ip, endpoint, "Success");
            return result;
        } catch (Throwable ex) {
            logEvent(auditEvent.value(), userId, ip, endpoint, "Error: " + ex.getMessage());
            throw ex;
        }
    }

    public void logEvent(String eventType, Long userId, String ip, String endpoint, String details) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setEventType(eventType);
        log.setUserId(userId);
        log.setIpAddress(ip);
        log.setEndpoint(endpoint);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
