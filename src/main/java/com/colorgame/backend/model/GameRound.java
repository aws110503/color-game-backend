package com.colorgame.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "grid_size", nullable = false, length = 10)
    private String gridSize;

    @Column(name = "exposure_time", nullable = false)
    private Integer exposureTime;

    @Column(name = "player_score", nullable = false)
    private Integer playerScore;

    @Column(name = "dominant_mistake", length = 100)
    private String dominantMistake;

    @Column(name = "ai_verdict", columnDefinition = "TEXT")
    private String aiVerdict;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "target_grid", columnDefinition = "jsonb")
    private String targetGrid;

    @PrePersist
    protected void onCreate() {
        this.playedAt = LocalDateTime.now();
    }
}