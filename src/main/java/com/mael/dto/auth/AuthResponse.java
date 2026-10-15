package com.mael.dto.auth;

public record AuthResponse(
        String token,
        long expiresInMs,
        UserInfo user
) {
}
