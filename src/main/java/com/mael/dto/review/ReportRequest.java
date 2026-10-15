package com.mael.dto.review;

import com.mael.entity.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * targetType: LESSON, QUESTION, MESSAGE, SOLUTION. targetId: id của đối tượng tương ứng.
 */
public record ReportRequest(
        @NotNull TargetType targetType,
        @NotBlank String targetId,
        @NotNull ReportReason reason,
        @Size(max = 2000) String description
) {
    public enum TargetType { LESSON, QUESTION, MESSAGE, SOLUTION }
}
