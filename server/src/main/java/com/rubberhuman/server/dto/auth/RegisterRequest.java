package com.rubberhuman.server.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email; // 可选
}