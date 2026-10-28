package com.mael.dto.lecturer;

import com.mael.entity.enums.ReviewDecision;

import java.time.LocalDateTime;

/** 1 lần duyệt: type = LESSON | QUESTION */
public record ReviewHistoryItem(
        String id,
        String type,
        String targetId,
        String targetTitle,
        String topicName,
        ReviewDecision decision,
        String comment,
        String lecturerName,
        LocalDateTime reviewedAt
) {
}
