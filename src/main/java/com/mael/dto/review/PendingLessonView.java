package com.mael.dto.review;

import com.mael.entity.enums.ReviewStatus;

import java.time.LocalDateTime;

public record PendingLessonView(
        String id,
        String title,
        String content,
        String topicName,
        Integer gradeLevel,
        ReviewStatus reviewStatus,
        LocalDateTime updatedAt
) {
}
