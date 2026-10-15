package com.mael.dto.learning;

public record LearningPathSummary(
        String id,
        String name,
        Integer gradeLevel,
        String description,
        int topicCount
) {
}
