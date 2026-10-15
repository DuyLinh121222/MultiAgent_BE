package com.mael.dto.learning;

import com.mael.entity.enums.ProgressStatus;

import java.util.List;

public record LessonDetail(
        String id,
        String title,
        String content,
        String videoUrl,
        Integer durationMinutes,
        String topicId,
        String topicName,
        Integer gradeLevel,
        ProgressStatus progressStatus,
        int progressPercent,
        List<MaterialView> materials
) {
}
