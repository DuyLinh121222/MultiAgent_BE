package com.mael.dto.lecturer;

import com.mael.entity.Misconception;
import com.mael.entity.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record LessonAdminView(
        String id,
        String topicId,
        String topicName,
        int gradeLevel,
        String title,
        String content,
        String videoUrl,
        Integer durationMinutes,
        List<Misconception> misconceptions,
        int orderIndex,
        ReviewStatus reviewStatus,
        String reviewedByName,
        LocalDateTime reviewedAt,
        LocalDateTime updatedAt
) {
}
