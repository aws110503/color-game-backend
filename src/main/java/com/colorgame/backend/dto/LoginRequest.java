package com.colorgame.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "L'identifiant est requis")
    @Size(max = 255, message = "L'identifiant est trop long")
    private String identifier;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(max = 128, message = "Le mot de passe est trop long")
    private String password;
}
