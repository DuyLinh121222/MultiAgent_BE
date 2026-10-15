package com.mael.dto.learning;

import com.mael.entity.enums.ProgressStatus;

public record LessonSummary(
        String id,
        String title,
        Integer durationMinutes,
        Integer orderIndex,
        ProgressStatus progressStatus,
        int progressPercent
) {
}
