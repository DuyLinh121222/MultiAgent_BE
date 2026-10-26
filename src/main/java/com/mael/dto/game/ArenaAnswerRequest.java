package com.mael.dto.game;

import jakarta.validation.constraints.NotBlank;

public record ArenaAnswerRequest(
        @NotBlank String questionId,
        @NotBlank String answer
) {
}
