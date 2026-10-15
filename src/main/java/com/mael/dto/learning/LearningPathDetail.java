package com.mael.dto.learning;

import java.util.List;

public record LearningPathDetail(
        String id,
        String name,
        Integer gradeLevel,
        String description,
        int completedLessons,
        int totalLessons,
        List<TopicView> topics
) {
}
