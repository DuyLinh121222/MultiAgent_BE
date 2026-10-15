package com.mael.dto.learning;

public record ContinueLesson(
        String lessonId,
        String lessonTitle,
        String topicName,
        int progressPercent
) {
}
