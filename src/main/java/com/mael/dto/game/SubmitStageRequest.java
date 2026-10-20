package com.mael.dto.game;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * answers: questionId -> chữ cái đáp án ("A", "B", ...)
 */
public record SubmitStageRequest(
        @NotNull(message = "Thiếu câu trả lời") Map<String, String> answers
) {
}
