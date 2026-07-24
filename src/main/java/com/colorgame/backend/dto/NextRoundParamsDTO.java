package com.colorgame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class NextRoundParamsDTO {
    private String difficulty;
    private String recommendedGridSize;
    private Double averageRecentScore;
    private Integer roundsConsidered;
}