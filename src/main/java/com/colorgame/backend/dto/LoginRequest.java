package com.colorgame.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier; // username OR email
    private String password;
}