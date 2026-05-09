package com.example.attendance_Backend.dto;

import java.util.Map;

public class AuthResponse {
    private final String token;
    private final String type = "Bearer";
    private final String role;
    private final Map<String, Object> user;

    public AuthResponse(String token, String role, Map<String, Object> user) {
        this.token = token;
        this.role = role;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public String getRole() {
        return role;
    }

    public Map<String, Object> getUser() {
        return user;
    }
}
