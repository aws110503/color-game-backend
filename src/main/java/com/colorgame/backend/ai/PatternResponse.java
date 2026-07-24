package com.colorgame.backend.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class PatternResponse {
    private List<List<String>> grid;
    private String structureType;
}