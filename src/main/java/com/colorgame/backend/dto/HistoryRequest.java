package com.colorgame.backend.dto;

import lombok.Data;

@Data
public class HistoryRequest {
    private Integer rowIndex;
    private Integer colIndex;
    private String color;
}