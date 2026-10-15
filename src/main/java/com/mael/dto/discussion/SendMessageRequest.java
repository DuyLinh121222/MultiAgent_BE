package com.mael.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Nội dung không được để trống") @Size(max = 2000) String content,
        String replyToId
) {
}
