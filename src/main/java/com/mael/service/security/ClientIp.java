package com.mael.service.security;

import jakarta.servlet.http.HttpServletRequest;

/** Lấy IP người gửi (ưu tiên X-Forwarded-For khi chạy sau proxy / ngrok) */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }
}
