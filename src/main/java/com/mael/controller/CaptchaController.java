package com.mael.controller;

import com.mael.service.security.CaptchaService;
import com.mael.service.security.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mã xác nhận hình cho form đăng ký / tư vấn (public) */
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping
    public CaptchaService.Captcha create(HttpServletRequest request) {
        return captchaService.create(ClientIp.of(request));
    }
}
