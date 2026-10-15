package com.mael.dto.learning;

import java.math.BigDecimal;
import java.util.List;

/**
 * masteryScore: mức nắm vững (0-100) lấy từ WEAKNESS, null nếu chưa có dữ liệu.
 */
public record TopicView(
        String id,
        String name,
        String description,
        Integer orderIndex,
        int completedLessons,
        int totalLessons,
        BigDecimal masteryScore,
        List<LessonSummary> lessons
) {
}
