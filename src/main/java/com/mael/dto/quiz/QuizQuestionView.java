package com.mael.dto.quiz;

import com.mael.entity.enums.QuestionType;

import java.math.BigDecimal;
import java.util.List;

/** Câu hỏi khi đang làm bài: không có đáp án đúng */
public record QuizQuestionView(
        String quizQuestionId,
        Integer orderIndex,
        String content,
        QuestionType questionType,
        List<String> options,
        BigDecimal points
) {
}
