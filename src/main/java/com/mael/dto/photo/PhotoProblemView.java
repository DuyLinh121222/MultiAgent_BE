package com.mael.dto.photo;

import com.mael.entity.enums.PhotoStatus;

import java.time.LocalDateTime;

public record PhotoProblemView(
        String id,
        String imageUrl,
        String extractedText,
        PhotoStatus status,
        String topicId,
        String topicName,
        LocalDateTime uploadedAt,
        SolutionView solution
) {
}
