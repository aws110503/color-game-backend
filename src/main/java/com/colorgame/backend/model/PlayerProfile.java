package com.colorgame.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_games")
    private Integer totalGames = 0;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "known_weaknesses", columnDefinition = "jsonb")
    private String knownWeaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "known_strengths", columnDefinition = "jsonb")
    private String knownStrengths;

    @Column(name = "current_difficulty", length = 50)
    private String currentDifficulty = "BEGINNER";

    @Column(name = "last_coached_at")
    private LocalDateTime lastCoachedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}