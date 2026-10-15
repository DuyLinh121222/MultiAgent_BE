package com.mael.dto.quiz;

import java.time.LocalDateTime;

public record StartAttemptResponse(
        String attemptId,
        LocalDateTime startedAt,
        QuizDetail quiz
) {
}
