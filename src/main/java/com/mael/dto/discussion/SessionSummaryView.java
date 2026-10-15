package com.mael.dto.discussion;

import com.mael.entity.enums.SessionStatus;

import java.time.LocalDateTime;

public record SessionSummaryView(
        String id,
        String title,
        String lessonId,
        String lessonTitle,
        SessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
