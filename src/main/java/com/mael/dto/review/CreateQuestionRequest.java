package com.mael.dto.review;

import com.mael.entity.enums.Difficulty;
import com.mael.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateQuestionRequest(
        @NotBlank String topicId,
        @NotBlank String content,
        @NotNull QuestionType questionType,
        List<String> options,
        @NotBlank String correctAnswer,
        String explanation,
        Difficulty difficulty
) {
}
