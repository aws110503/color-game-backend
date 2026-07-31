package com.colorgame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
public class CoachingDTO {
    private String headline;
    private List<String> observations;
    private List<String> drills;
    private String encouragement;
}