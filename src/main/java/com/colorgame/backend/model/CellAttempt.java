package com.colorgame.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "cell_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CellAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private GameRound round;

    @Column(name = "cell_row", nullable = false)
    private Integer cellRow;

    @Column(name = "cell_col", nullable = false)
    private Integer cellCol;

    @Column(name = "target_color", nullable = false, length = 50)
    private String targetColor;

    @Column(name = "player_color", nullable = false, length = 50)
    private String playerColor;

    @Column(name = "is_exact_match", nullable = false)
    private Boolean isExactMatch;

    @Column(name = "is_family_match", nullable = false)
    private Boolean isFamilyMatch;
}