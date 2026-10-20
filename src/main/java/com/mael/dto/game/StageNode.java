package com.mael.dto.game;

/**
 * Một ải thường trên bản đồ (tương ứng 1 bài học).
 *
 * @param levelNumber số thứ tự ải trên toàn bản đồ (1, 2, 3...)
 * @param status      LOCKED | OPEN | CLEARED
 */
public record StageNode(
        String lessonId,
        int levelNumber,
        String title,
        int stars,
        String status,
        boolean current,
        int questionCount,
        Integer durationMinutes
) {
}
