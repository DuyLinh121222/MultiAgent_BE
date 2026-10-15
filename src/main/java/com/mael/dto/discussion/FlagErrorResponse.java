package com.mael.dto.discussion;

/**
 * Kết quả khi học sinh bấm "Bạn ấy nói sai!" trên một tin nhắn của AI.
 */
public record FlagErrorResponse(
        boolean correct,
        String feedback,
        boolean achievementUnlocked
) {
}
