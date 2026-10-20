package com.mael.dto.discussion;

/**
 * Kết quả khi học sinh bấm "Bạn ấy nói sai!" trên một tin nhắn của AI.
 *
 * @param explanation giải thích chỗ sai (chỉ có khi học sinh bắt đúng lỗi)
 */
public record FlagErrorResponse(
        boolean correct,
        String feedback,
        String explanation,
        boolean achievementUnlocked
) {
}
