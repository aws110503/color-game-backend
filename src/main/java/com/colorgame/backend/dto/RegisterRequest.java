package com.colorgame.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Le nom d'utilisateur est requis")
    @Size(min = 7, max = 100, message = "Le nom d'utilisateur doit contenir entre 7 et 100 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores")
    private String username;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    @Size(max = 255, message = "L'email est trop long")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    private String password;
}
