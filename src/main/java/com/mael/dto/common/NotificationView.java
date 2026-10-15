package com.mael.dto.common;

import com.mael.entity.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationView(
        String id,
        NotificationType type,
        String title,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
}
