package com.mael.controller;

import com.mael.dto.auth.AuthResponse;
import com.mael.dto.auth.LoginRequest;
import com.mael.dto.auth.RegisterRequest;
import com.mael.dto.auth.UserInfo;
import com.mael.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Học sinh đăng ký, tự động có 7 ngày dùng thử */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public UserInfo me() {
        return authService.me();
    }
}
