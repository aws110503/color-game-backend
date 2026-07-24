package com.colorgame.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_audit_log")
@Data
@NoArgsConstructor
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        this.occurredAt = LocalDateTime.now();
    }
}
