package com.mael.dto.game;

public record AnswerFeedback(
        String questionId,
        String content,
        String selectedAnswer,
        String correctAnswer,
        boolean correct,
        String explanation
) {
}
