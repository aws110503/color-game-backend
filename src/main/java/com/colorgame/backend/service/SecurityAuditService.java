package com.colorgame.backend.service;

import com.colorgame.backend.model.SecurityAuditLog;
import com.colorgame.backend.repository.SecurityAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;

    public SecurityAuditService(SecurityAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<SecurityAuditLog> getAuditLog(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAll(PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by("occurredAt").descending()));
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        LocalDateTime since1h = LocalDateTime.now().minusHours(1);

        stats.put("loginFailures24h", auditLogRepository.countLoginFailuresSince(since24h));
        stats.put("suspiciousIps", auditLogRepository.findSuspiciousIps(since1h, 10));
        stats.put("recentEvents", auditLogRepository.findRecentEvents(since24h).stream().limit(50).toList());

        return stats;
    }

    public List<Map<String, Object>> getSuspiciousIps() {
        List<Object[]> results = auditLogRepository.findSuspiciousIps(LocalDateTime.now().minusHours(1), 10);
        return results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("ipAddress", row[0]);
            map.put("failureCount", row[1]);
            return map;
        }).toList();
    }
}
