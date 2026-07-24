package com.colorgame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ProfileDTO {
    private Integer totalGames;
    private BigDecimal averageScore;
    private List<String> knownWeaknesses;
    private List<String> knownStrengths;
    private String currentDifficulty;
}