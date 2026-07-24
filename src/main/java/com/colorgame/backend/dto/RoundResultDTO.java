package com.colorgame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
public class RoundResultDTO {
    private Long roundId;
    private Integer playerScore;
    private String verdict;
    private String dominantMistake;
    private List<List<Boolean>> exactMatchHeatmap;
    private List<List<Boolean>> familyMatchHeatmap;
}