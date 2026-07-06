package com.colorgame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HistoryDTO {
    private Long id;
    private String username;
    private Integer rowIndex;
    private Integer colIndex;
    private String color;
    private LocalDateTime changedAt;
}