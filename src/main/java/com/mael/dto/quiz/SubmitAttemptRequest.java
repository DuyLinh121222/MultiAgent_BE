package com.mael.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAttemptRequest(
        @NotNull List<@Valid AnswerItem> answers
) {
    public record AnswerItem(
            @NotBlank String quizQuestionId,
            String selectedAnswer
    ) {
    }
}
