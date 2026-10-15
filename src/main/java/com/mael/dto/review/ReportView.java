package com.mael.dto.review;

import com.mael.entity.enums.ReportReason;
import com.mael.entity.enums.ReportStatus;

import java.time.LocalDateTime;

public record ReportView(
        String id,
        String studentName,
        String targetType,
        String targetId,
        String targetPreview,
        ReportReason reason,
        String description,
        ReportStatus status,
        String resolutionNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
