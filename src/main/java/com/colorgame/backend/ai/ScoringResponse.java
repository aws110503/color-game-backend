package com.colorgame.backend.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ScoringResponse {
    private Integer score;
    private String dominantMistake;
    private String verdict;
    private List<List<CellResult>> cellResults;

    @Data
    @NoArgsConstructor
    public static class CellResult {
        private Boolean exactMatch;
        private Boolean familyMatch;
    }
}