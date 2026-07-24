package com.colorgame.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HistoryRequest {
    @NotBlank(message = "La ligne est requise")
    private Integer rowIndex;

    @NotBlank(message = "La colonne est requise")
    private Integer colIndex;

    @NotBlank(message = "La couleur est requise")
    private String color;
}
