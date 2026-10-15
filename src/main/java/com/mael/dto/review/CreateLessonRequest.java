package com.mael.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLessonRequest(
        @NotBlank String topicId,
        @NotBlank @Size(max = 255) String title,
        String content,
        String videoUrl,
        Integer durationMinutes
) {
}
