package com.colorgame.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundSubmitRequest {
    private Long roundId;
    private List<List<String>> playerGrid;
}