package com.mael.dto.quiz;

import com.mael.entity.enums.QuizType;

import java.util.List;

public record QuizDetail(
        String id,
        String title,
        QuizType quizType,
        Integer timeLimitMinutes,
        List<QuizQuestionView> questions
) {
}
