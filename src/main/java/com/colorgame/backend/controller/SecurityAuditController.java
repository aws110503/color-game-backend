package com.colorgame.backend.controller;

import com.colorgame.backend.model.SecurityAuditLog;
import com.colorgame.backend.service.SecurityAuditService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/security")
public class SecurityAuditController {

    private final SecurityAuditService securityAuditService;

    public SecurityAuditController(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<SecurityAuditLog>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(securityAuditService.getAuditLog(page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(securityAuditService.getStats());
    }

    @GetMapping("/suspicious-ips")
    public ResponseEntity<List<Map<String, Object>>> getSuspiciousIps() {
        return ResponseEntity.ok(securityAuditService.getSuspiciousIps());
    }
}
