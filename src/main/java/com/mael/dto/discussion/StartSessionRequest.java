package com.mael.dto.discussion;

import jakarta.validation.constraints.NotBlank;

public record StartSessionRequest(
        @NotBlank(message = "Vui lòng chọn bài học") String lessonId
) {
}
