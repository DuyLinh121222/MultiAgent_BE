package com.mael.controller;

import com.mael.service.tts.TtsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Giọng đọc riêng cho 3 AI.
 */
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    /** FE hỏi xem BE có giọng Google không; không có thì dùng giọng trình duyệt */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("enabled", ttsService.enabled(), "voices", ttsService.voices());
    }

    @PostMapping(produces = "audio/mpeg")
    public ResponseEntity<byte[]> speak(@Valid @RequestBody SpeakRequest req) {
        if (!ttsService.enabled()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .body(ttsService.synthesize(req.agentId(), req.text()));
    }

    public record SpeakRequest(@NotBlank String agentId, @NotBlank @Size(max = 1500) String text) {
    }

    @GetMapping("/voices")
    public List<TtsService.VoiceInfo> voices() {
        return ttsService.voices();
    }
}
