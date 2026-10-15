package com.mael.dto.quiz;

import java.math.BigDecimal;
import java.util.List;

public record AttemptResult(
        String attemptId,
        String quizTitle,
        BigDecimal score,
        BigDecimal maxScore,
        int correctCount,
        int totalQuestions,
        List<AnswerResult> results,
        WeaknessView weakness,
        boolean achievementUnlocked
) {
    public record AnswerResult(
            String quizQuestionId,
            String content,
            String selectedAnswer,
            String correctAnswer,
            boolean correct,
            String explanation
    ) {
    }
}
