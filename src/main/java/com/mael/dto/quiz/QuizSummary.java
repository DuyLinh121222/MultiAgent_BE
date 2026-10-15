package com.mael.dto.quiz;

import com.mael.entity.enums.QuizType;

import java.math.BigDecimal;

public record QuizSummary(
        String id,
        String title,
        QuizType quizType,
        Integer timeLimitMinutes,
        int questionCount,
        String weaknessTopicName,
        BigDecimal bestScore
) {
}
