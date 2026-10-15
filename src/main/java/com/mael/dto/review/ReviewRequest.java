package com.mael.dto.review;

import com.mael.entity.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "Vui lòng chọn duyệt hoặc từ chối") ReviewDecision decision,
        @Size(max = 2000) String comment
) {
}
