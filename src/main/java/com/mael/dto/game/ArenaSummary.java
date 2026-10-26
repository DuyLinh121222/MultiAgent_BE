package com.mael.dto.game;

/**
 * Một đấu trường (mỗi chương một đấu trường, câu hỏi lấy từ ngân hàng câu hỏi đã duyệt của chương).
 */
public record ArenaSummary(
        String topicId,
        int orderIndex,
        String name,
        boolean unlocked,
        int questionCount,
        int bestScore,
        String rewardPetCode
) {
}
