package com.mael.dto.insight;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phân tích cá nhân hoá cho học sinh (ngắn, ít chữ, kèm nút đi thẳng tới ải nên chơi).
 *
 * @param mood        HAPPY | FOCUS | START  (FE chọn biểu cảm thú cưng)
 * @param generatedBy AI (Gemini) hoặc RULE (tự tính khi chưa bật AI)
 */
public record InsightView(
        String headline,
        String mood,
        List<String> strengths,
        List<Focus> focus,
        String tip,
        int masteryPercent,
        List<ChapterSkill> chapters,
        String generatedBy,
        LocalDateTime generatedAt
) {
    /** Việc nên làm tiếp: targetType LESSON | BOSS | ARENA */
    public record Focus(String title, String reason, String targetType, String targetId) {
    }

    /** Mức thành thạo theo chương (0-100) để vẽ biểu đồ */
    public record ChapterSkill(String topicId, String name, int percent, int stars, int starsTotal) {
    }
}
