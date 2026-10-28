package com.mael.dto.admin;

import java.time.LocalDateTime;

public record AuditLogView(
        String id,
        String actorName,
        String action,
        String targetType,
        String targetId,
        String detail,
        LocalDateTime createdAt
) {
}
