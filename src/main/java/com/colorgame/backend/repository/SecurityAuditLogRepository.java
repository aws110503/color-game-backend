package com.colorgame.backend.repository;

import com.colorgame.backend.model.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    List<SecurityAuditLog> findByOrderByOccurredAtDesc();

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.eventType = :type ORDER BY s.occurredAt DESC")
    List<SecurityAuditLog> findByEventType(@Param("type") String eventType);

    @Query("SELECT s FROM SecurityAuditLog s WHERE s.occurredAt >= :since ORDER BY s.occurredAt DESC")
    List<SecurityAuditLog> findRecentEvents(@Param("since") LocalDateTime since);

    @Query("SELECT s.ipAddress, COUNT(s) as failCount FROM SecurityAuditLog s " +
           "WHERE s.eventType = 'LOGIN_FAILURE' AND s.occurredAt >= :since " +
           "GROUP BY s.ipAddress HAVING COUNT(s) >= :minCount ORDER BY failCount DESC")
    List<Object[]> findSuspiciousIps(@Param("since") LocalDateTime since, @Param("minCount") long minCount);

    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.eventType = 'LOGIN_FAILURE' AND s.occurredAt >= :since")
    long countLoginFailuresSince(@Param("since") LocalDateTime since);
}
